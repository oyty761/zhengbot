package com.guzheng.voice.controller;

import com.guzheng.voice.dto.VoiceInteractionDtos;
import com.guzheng.voice.service.VoiceInteractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoiceInteractionController.class)
@Import(VoiceInteractionController.class)
class VoiceInteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VoiceInteractionService voiceInteractionService;

    @Test
    void transcriptionUsesMultipartContract() throws Exception {
        VoiceInteractionDtos.TranscriptionResponse response =
                new VoiceInteractionDtos.TranscriptionResponse(
                        1002L,
                        "550e8400-e29b-41d4-a716-446655440000",
                        901L,
                        "播放渔舟唱晚",
                        "zh-CN",
                        new BigDecimal("0.9600"),
                        1840,
                        "COMPLETED",
                        "SONG_SEARCH",
                        new BigDecimal("0.9800"),
                        new VoiceInteractionDtos.SongEntities("渔舟唱晚", null),
                        null,
                        null,
                        false,
                        null,
                        null);
        when(voiceInteractionService.transcribe(any(), any(), eq("zh-CN"), eq(1840)))
                .thenReturn(response);

        MockMultipartFile audio = new MockMultipartFile(
                "audioFile", "voice.webm", "audio/webm", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/voice-interaction/transcriptions")
                        .file(audio)
                        .param("vadDurationMs", "1840"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.intentType").value("SONG_SEARCH"))
                .andExpect(jsonPath("$.data.content").value("播放渔舟唱晚"));
    }

    @Test
    void blankQuestionReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/voice-interaction/qa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("问题内容不能为空"));
    }
}
