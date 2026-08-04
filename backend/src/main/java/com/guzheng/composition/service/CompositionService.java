package com.guzheng.composition.service;

import com.guzheng.common.BusinessException;
import com.guzheng.composition.dto.CompositionDtos;
import com.guzheng.composition.model.JptScore;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Composition application service.
 *
 * <p>The current robot transport is not available, so editing state and compiled
 * JPT assets use an in-process store. The API boundary is stable; the store can
 * later be replaced by a MyBatis repository without changing the frontend.</p>
 */
@Service
public class CompositionService {
    private static final int DEFAULT_TICKS=480;
    private final JptCodec codec;
    private final AtomicLong compositionIds=new AtomicLong(1000), noteIds=new AtomicLong(1);
    private final AtomicLong completionIds=new AtomicLong(1), assetIds=new AtomicLong(1), runIds=new AtomicLong(1);
    private final Map<Long,Workspace> workspaces=new ConcurrentHashMap<>();
    private final Map<Long,Completion> completions=new ConcurrentHashMap<>();
    private final Map<Long,String> assets=new ConcurrentHashMap<>();
    private final Map<Long,Run> runs=new ConcurrentHashMap<>();

    public CompositionService(JptCodec codec){this.codec=codec;}

    public CompositionDtos.InitResponse init(){
        long id=compositionIds.incrementAndGet();Workspace w=new Workspace(id);workspaces.put(id,w);return view(w);
    }

    public CompositionDtos.NoteResponse add(CompositionDtos.NoteInput in){
        Workspace w=editable(in.compositionId());validateNote(in.stringNo(),in.startTick(),in.durationTick(),in.velocity());
        synchronized(w){rejectCollision(w,in.stringNo(),in.startTick(),null);long id=noteIds.incrementAndGet();w.notes.add(new Note(id,in.stringNo(),in.startTick(),in.durationTick(),defaultVelocity(in.velocity()),defaultTechnique(in.technique()),defaultHand(in.hand()),null,"ACTIVE"));return new CompositionDtos.NoteResponse(id,"音符创建成功");}
    }

    public CompositionDtos.NoteResponse update(long noteId,CompositionDtos.NoteInput in){
        Workspace w=editable(in.compositionId());validateNote(in.stringNo(),in.startTick(),in.durationTick(),in.velocity());
        synchronized(w){Note old=w.notes.stream().filter(n->n.id==noteId).findFirst().orElseThrow(()->new BusinessException(404,"音符不存在"));rejectCollision(w,in.stringNo(),in.startTick(),noteId);w.notes.remove(old);w.notes.add(new Note(noteId,in.stringNo(),in.startTick(),in.durationTick(),defaultVelocity(in.velocity()),defaultTechnique(in.technique()),defaultHand(in.hand()),old.aiCompletionNo,"ACTIVE"));return new CompositionDtos.NoteResponse(noteId,"音符更新成功");}
    }

    public CompositionDtos.MessageResponse delete(long noteId,long compositionId){
        Workspace w=editable(compositionId);synchronized(w){if(!w.notes.removeIf(n->n.id==noteId))throw new BusinessException(404,"音符不存在");}return new CompositionDtos.MessageResponse("音符已删除");
    }

    public CompositionDtos.ScoreResponse replaceScore(long id,CompositionDtos.ScoreInput score){
        Workspace w=editable(id);if(!score.meter().matches("(2|3|4)/4|6/8"))throw new BusinessException(400,"仅支持 2/4、3/4、4/4、6/8 拍");
        List<Note> replacement=new ArrayList<>();
        for(CompositionDtos.ScoreNote n:score.notes()){
            validateNote(n.stringNo(),n.startTick(),n.durationTick(),n.velocity());
            boolean collision=replacement.stream().anyMatch(x->x.stringNo==n.stringNo()&&x.startTick==n.startTick());if(collision)throw new BusinessException(409,"同一弦同一时刻不能放置多个音符");
            replacement.add(new Note(n.noteId()==null?noteIds.incrementAndGet():n.noteId(),n.stringNo(),n.startTick(),n.durationTick(),defaultVelocity(n.velocity()),defaultTechnique(n.technique()),defaultHand(n.hand()),null,"ACTIVE"));
        }
        synchronized(w){w.title=score.title();w.tempo=score.tempo();w.meter=score.meter();w.ticks=score.ticksPerBeat();w.notes.clear();w.notes.addAll(replacement);}
        return new CompositionDtos.ScoreResponse(id,replacement.size(),"乐谱同步成功");
    }

    public String exportJpt(long id){Workspace w=workspace(id);synchronized(w){return codec.write(toJpt(w));}}

    public CompositionDtos.ScoreResponse importJpt(long id,String text){
        JptScore score=codec.parse(text);Workspace w=editable(id);int beats=Integer.parseInt(score.meter().split("/")[0]);int end=score.notes().stream().mapToInt(n->n.t()+n.dur()).max().orElse(0);if(Math.ceil((double)end/score.ticks()/beats)>16)throw new BusinessException(400,"乐谱超过 16 小节");
        List<Note> replacement=score.notes().stream().map(n->new Note(noteIds.incrementAndGet(),n.string(),n.t(),n.dur(),n.velocity(),n.technique(),n.hand(),null,"ACTIVE")).toList();
        synchronized(w){w.title=score.title();w.tempo=score.tempo();w.meter=score.meter();w.ticks=score.ticks();w.notes.clear();w.notes.addAll(replacement);}
        return new CompositionDtos.ScoreResponse(id,replacement.size(),"JPT 校验并导入成功");
    }

    public CompositionDtos.AiCompletionResponse suggest(long compositionId){
        Workspace w=editable(compositionId);List<Note> suggestions=new ArrayList<>();long cid=completionIds.incrementAndGet();
        synchronized(w){int tick=w.notes.stream().mapToInt(n->n.startTick+n.durationTick).max().orElse(0);int base=w.notes.stream().max(Comparator.comparingInt(n->n.startTick)).map(n->n.stringNo).orElse(11);for(int i=0;i<4;i++){int stringNo=Math.max(1,Math.min(21,base+(i%2==0?1:-1)));suggestions.add(new Note(noteIds.incrementAndGet(),stringNo,tick+i*w.ticks,w.ticks,76,"pluck","auto",(int)cid,"PROPOSED"));}}
        Completion c=new Completion(cid,compositionId,suggestions);completions.put(cid,c);return new CompositionDtos.AiCompletionResponse(cid,suggestions.stream().map(this::view).toList());
    }

    public CompositionDtos.MessageResponse decide(long completionId,CompositionDtos.AiDecisionRequest request){
        Completion c=completions.get(completionId);if(c==null||c.compositionId!=request.compositionId())throw new BusinessException(404,"AI 补全批次不存在");if(request.noteIndex()>=c.notes.size())throw new BusinessException(400,"noteIndex 超出范围");
        if(!List.of("accept","reject").contains(request.action().toLowerCase()))throw new BusinessException(400,"action 应为 accept 或 reject");
        if("accept".equalsIgnoreCase(request.action())){Workspace w=editable(request.compositionId());Note n=c.notes.get(request.noteIndex());synchronized(w){rejectCollision(w,n.stringNo,n.startTick,null);w.notes.add(n.withState("ACTIVE"));}}
        return new CompositionDtos.MessageResponse("accept".equalsIgnoreCase(request.action())?"已接受补全音符":"已拒绝补全音符");
    }

    public CompositionDtos.CompleteResponse complete(long compositionId){
        Workspace w=editable(compositionId);synchronized(w){if(w.notes.isEmpty())throw new BusinessException(400,"乐谱至少需要一个音符");long assetId=assetIds.incrementAndGet();assets.put(assetId,codec.write(toJpt(w)));w.status="LOCKED";w.commandAssetId=assetId;return new CompositionDtos.CompleteResponse(w.id,w.status,assetId,"乐谱已锁定，JPT 指令资源编译完成");}
    }

    public CompositionDtos.PerformanceResponse startPerformance(CompositionDtos.PerformanceRequest request){
        Workspace w=workspace(request.compositionId());if(!"LOCKED".equals(w.status))throw new BusinessException(409,"请先完成并锁定乐谱");if(!request.commandAssetId().equals(w.commandAssetId)||!assets.containsKey(request.commandAssetId()))throw new BusinessException(400,"指令资源与作品不匹配");
        long id=runIds.incrementAndGet();Run run=new Run(id,LocalDateTime.now());runs.put(id,run);return runView(run);
    }

    public CompositionDtos.PerformanceResponse performance(long runId){Run run=runs.get(runId);if(run==null)throw new BusinessException(404,"演奏任务不存在");return runView(run);}

    private CompositionDtos.PerformanceResponse runView(Run run){
        long elapsed=Duration.between(run.requestedAt,LocalDateTime.now()).toMillis();String status=elapsed<1000?"QUEUED":elapsed<4000?"PLAYING":"SUCCEEDED";LocalDateTime started=elapsed>=1000?run.requestedAt.plusSeconds(1):null;LocalDateTime ended=elapsed>=4000?run.requestedAt.plusSeconds(4):null;return new CompositionDtos.PerformanceResponse(run.id,status,run.requestedAt,started,ended,ended==null?null:3000L,status.equals("SUCCEEDED")?"模拟演奏完成":"演奏任务处理中");
    }

    private Workspace workspace(long id){Workspace w=workspaces.get(id);if(w==null)throw new BusinessException(404,"创作不存在或服务已重启，请重新初始化");return w;}
    private Workspace editable(long id){Workspace w=workspace(id);if(!"EDITING".equals(w.status))throw new BusinessException(409,"乐谱已锁定，不能继续编辑");return w;}
    private void rejectCollision(Workspace w,int stringNo,int startTick,Long except){if(w.notes.stream().anyMatch(n->n.stringNo==stringNo&&n.startTick==startTick&&(except==null||n.id!=except)))throw new BusinessException(409,"这个弦位和时刻已有音符");}
    private static void validateNote(Integer stringNo,Integer start,Integer duration,Integer velocity){if(stringNo==null||stringNo<1||stringNo>21)throw new BusinessException(400,"stringNo 必须在 1–21");if(start==null||start<0)throw new BusinessException(400,"startTick 不能小于0");if(duration==null||duration<=0)throw new BusinessException(400,"durationTick 必须大于0");if(velocity!=null&&(velocity<1||velocity>127))throw new BusinessException(400,"velocity 必须在 1–127");}
    private static int defaultVelocity(Integer value){return value==null?80:value;}private static String defaultTechnique(String value){return value==null||value.isBlank()?"pluck":value;}private static String defaultHand(String value){return value==null||value.isBlank()?"auto":value;}
    private CompositionDtos.InitResponse view(Workspace w){return new CompositionDtos.InitResponse(w.id,w.ticks,w.status,w.title,w.tempo,w.meter,w.notes.stream().map(this::view).toList());}
    private CompositionDtos.NoteView view(Note n){return new CompositionDtos.NoteView(n.id,n.stringNo,n.startTick,n.durationTick,n.velocity,n.technique,n.hand,n.aiCompletionNo,n.state);}
    private JptScore toJpt(Workspace w){return new JptScore(w.title,"",w.tempo,w.meter,w.ticks,"D-pentatonic",w.notes.stream().filter(n->"ACTIVE".equals(n.state)).map(n->new JptScore.JptNote(n.startTick,n.durationTick,n.stringNo,JptCodec.PITCHES.get(n.stringNo-1),n.velocity,n.technique,n.hand)).toList());}

    private static final class Workspace{final long id;String title="我的古筝作品",status="EDITING",meter="4/4";int tempo=120,ticks=DEFAULT_TICKS;Long commandAssetId;final List<Note> notes=new ArrayList<>();Workspace(long id){this.id=id;}}
    private record Note(long id,int stringNo,int startTick,int durationTick,int velocity,String technique,String hand,Integer aiCompletionNo,String state){Note withState(String next){return new Note(id,stringNo,startTick,durationTick,velocity,technique,hand,aiCompletionNo,next);}}
    private record Completion(long id,long compositionId,List<Note> notes){}
    private record Run(long id,LocalDateTime requestedAt){}
}
