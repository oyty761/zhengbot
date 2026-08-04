package com.guzheng.composition.service;

import com.guzheng.common.BusinessException;
import com.guzheng.composition.dto.CompositionDtos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompositionServiceTest {
    private final CompositionService service=new CompositionService(new JptCodec());

    @Test void supportsEditSyncCompileAndPerformanceFlow(){
        var init=service.init();
        var note=service.add(new CompositionDtos.NoteInput(null,init.compositionId(),11,0,480,80,"pluck","R"));
        assertNotNull(note.noteId());
        service.replaceScore(init.compositionId(),new CompositionDtos.ScoreInput("测试作品",100,"4/4",480,List.of(new CompositionDtos.ScoreNote(note.noteId(),11,0,480,80,"pluck","R"))));
        var complete=service.complete(init.compositionId());assertEquals("LOCKED",complete.editStatus());
        var run=service.startPerformance(new CompositionDtos.PerformanceRequest(init.compositionId(),complete.commandAssetId()));assertEquals("QUEUED",run.runStatus());
        assertThrows(BusinessException.class,()->service.add(new CompositionDtos.NoteInput(null,init.compositionId(),12,480,480,80,"pluck","L")));
    }

    @Test void importsAndExportsJpt(){
        long id=service.init().compositionId();String text="""
                JPT 1.0
                META title="导入测试" composer="" tempo=90 meter=4/4 ticks=480 tuning=D-pentatonic
                NOTE t=0 dur=480 string=1 pitch=D6 velocity=80 technique=pluck hand=R
                END
                """;
        assertEquals(1,service.importJpt(id,text).noteCount());assertEquals("导入测试",new JptCodec().parse(service.exportJpt(id)).title());
    }
}
