package com.guzheng.composition.service;

import com.guzheng.composition.model.JptScore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Strict parser/writer for the line-oriented JPT 1.0 UTF-8 format. */
@Component
public class JptCodec {
    public static final String VERSION = "1.0";
    public static final List<String> PITCHES = List.of(
            "D6","B5","A5","F#5","E5","D5","B4","A4","F#4","E4","D4",
            "B3","A3","F#3","E3","D3","B2","A2","F#2","E2","D2");
    private static final Set<String> TECHNIQUES = Set.of(
            "pluck","tremolo","glissando","press","slide","vibrato","mute");

    public JptScore parse(String source) {
        if (source == null) throw new IllegalArgumentException("JPT 内容不能为空");
        String title="未命名作品", composer="", meter="4/4", tuning="D-pentatonic";
        int tempo=120, ticks=480; boolean header=false, meta=false, end=false;
        List<JptScore.JptNote> notes=new ArrayList<>();
        String[] lines=source.replace("\uFEFF", "").split("\\R", -1);
        for(int i=0;i<lines.length;i++) {
            String line=lines[i].trim(); int lineNo=i+1;
            if(line.isEmpty()||line.startsWith("#")) continue;
            List<String> tokens=tokenize(line,lineNo); String type=tokens.remove(0).toUpperCase();
            if(end) throw error(lineNo,"END 后不能再有记录");
            if("JPT".equals(type)) {
                if(header) throw error(lineNo,"重复 JPT 文件头");
                if(tokens.size()!=1||!VERSION.equals(tokens.get(0))) throw error(lineNo,"仅支持 JPT "+VERSION);
                header=true; continue;
            }
            if(!header) throw error(lineNo,"之前缺少 JPT "+VERSION+" 文件头");
            if("META".equals(type)) {
                if(meta) throw error(lineNo,"重复 META 记录");
                Map<String,String> f=fields(tokens,lineNo);
                title=f.getOrDefault("title",title); composer=f.getOrDefault("composer",composer);
                tempo=integer(f.getOrDefault("tempo",String.valueOf(tempo)),"tempo",lineNo,20,300);
                meter=f.getOrDefault("meter",meter); if(!meter.matches("\\d+/(2|4|8|16)")) throw error(lineNo,"meter 格式应为 4/4");
                ticks=integer(f.getOrDefault("ticks",String.valueOf(ticks)),"ticks",lineNo,24,9600);
                tuning=f.getOrDefault("tuning",tuning); if(!"D-pentatonic".equals(tuning)) throw error(lineNo,"JPT 1.0 仅支持 tuning=D-pentatonic");
                meta=true; continue;
            }
            if("NOTE".equals(type)) {
                Map<String,String> f=fields(tokens,lineNo);
                int t=integer(required(f,"t",lineNo),"t",lineNo,0,Integer.MAX_VALUE);
                int dur=integer(required(f,"dur",lineNo),"dur",lineNo,1,Integer.MAX_VALUE);
                int stringNo=integer(required(f,"string",lineNo),"string",lineNo,1,21);
                int velocity=integer(f.getOrDefault("velocity","80"),"velocity",lineNo,1,127);
                String pitch=f.getOrDefault("pitch",PITCHES.get(stringNo-1));
                if(!PITCHES.get(stringNo-1).equals(pitch)) throw error(lineNo,"pitch 与弦 "+stringNo+"（"+PITCHES.get(stringNo-1)+"）不一致");
                String technique=f.getOrDefault("technique","pluck"); if(!TECHNIQUES.contains(technique)) throw error(lineNo,"不支持 technique="+technique);
                String hand=f.getOrDefault("hand","auto"); if(!Set.of("L","R","auto").contains(hand)) throw error(lineNo,"hand 应为 L、R 或 auto");
                notes.add(new JptScore.JptNote(t,dur,stringNo,pitch,velocity,technique,hand)); continue;
            }
            if("END".equals(type)) { if(!tokens.isEmpty()) throw error(lineNo,"END 不接受字段"); end=true; continue; }
            throw error(lineNo,"未知记录类型："+type);
        }
        if(!header) throw new IllegalArgumentException("缺少 JPT 1.0 文件头");
        if(!meta) throw new IllegalArgumentException("缺少 META 记录");
        if(!end) throw new IllegalArgumentException("缺少 END 记录");
        notes.sort(Comparator.comparingInt(JptScore.JptNote::t).thenComparingInt(JptScore.JptNote::string));
        return new JptScore(title,composer,tempo,meter,ticks,tuning,List.copyOf(notes));
    }

    public String write(JptScore score) {
        StringBuilder out=new StringBuilder("JPT 1.0\n");
        out.append("META title=").append(quote(score.title())).append(" composer=").append(quote(score.composer()))
                .append(" tempo=").append(score.tempo()).append(" meter=").append(score.meter())
                .append(" ticks=").append(score.ticks()).append(" tuning=").append(score.tuning()).append("\n\n");
        score.notes().stream().sorted(Comparator.comparingInt(JptScore.JptNote::t).thenComparingInt(JptScore.JptNote::string)).forEach(n -> out
                .append("NOTE t=").append(n.t()).append(" dur=").append(n.dur()).append(" string=").append(n.string())
                .append(" pitch=").append(PITCHES.get(n.string()-1)).append(" velocity=").append(n.velocity())
                .append(" technique=").append(n.technique()).append(" hand=").append(n.hand()).append('\n'));
        return out.append("\nEND\n").toString();
    }

    private static List<String> tokenize(String line,int lineNo) {
        List<String> out=new ArrayList<>(); StringBuilder cur=new StringBuilder(); boolean quoted=false,escaped=false;
        for(char ch:line.toCharArray()) {
            if(escaped){cur.append(ch);escaped=false;continue;} if(ch=='\\'&&quoted){escaped=true;continue;}
            if(ch=='\"'){quoted=!quoted;continue;} if(Character.isWhitespace(ch)&&!quoted){if(!cur.isEmpty()){out.add(cur.toString());cur.setLength(0);}}else cur.append(ch);
        }
        if(quoted) throw error(lineNo,"引号未闭合"); if(!cur.isEmpty())out.add(cur.toString()); return out;
    }
    private static Map<String,String> fields(List<String> tokens,int lineNo){Map<String,String> out=new HashMap<>();for(String token:tokens){int at=token.indexOf('=');if(at<1)throw error(lineNo,"字段缺少 =："+token);out.put(token.substring(0,at),token.substring(at+1));}return out;}
    private static String required(Map<String,String> f,String key,int lineNo){String v=f.get(key);if(v==null)throw error(lineNo,"缺少字段 "+key);return v;}
    private static int integer(String value,String name,int lineNo,int min,int max){try{long n=Long.parseLong(value);if(n<min||n>max)throw error(lineNo,name+" 超出范围 "+min+"–"+max);return(int)n;}catch(NumberFormatException e){throw error(lineNo,name+" 必须是整数");}}
    private static IllegalArgumentException error(int lineNo,String message){return new IllegalArgumentException("第 "+lineNo+" 行"+message);}
    private static String quote(String value){return "\""+(value==null?"":value.replace("\\","\\\\").replace("\"","\\\""))+"\"";}
}
