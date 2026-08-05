package com.guzheng.voice.service;

import com.guzheng.common.BusinessException;
import com.guzheng.songbook.dto.SongbookDtos;
import com.guzheng.songbook.service.SongbookService;
import com.guzheng.voice.ai.VoiceAiClient;
import com.guzheng.voice.config.VoiceApiProperties;
import com.guzheng.voice.dto.VoiceInteractionDtos;
import com.guzheng.voice.mapper.VoiceInteractionMapper;
import com.guzheng.voice.model.VoiceRecords;
import com.guzheng.voice.service.impl.VoiceInteractionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoiceInteractionServiceImplTest {

    @Mock
    private VoiceInteractionMapper voiceMapper;

    @Mock
    private VoiceAiClient voiceAiClient;

    @Mock
    private SongbookService songbookService;

    @Mock
    private VoiceApiProperties properties;

    @TempDir
    private Path temporaryDirectory;

    private VoiceInteractionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VoiceInteractionServiceImpl(
                voiceMapper,
                voiceAiClient,
                songbookService,
                properties);
    }

    @Test
    void matchedVoiceSongDirectlyCreatesPerformance() {
        when(properties.audioStorageDir()).thenReturn(temporaryDirectory.toString());
        when(voiceAiClient.transcribe(any(), any(), any(), any()))
                .thenReturn(new VoiceAiClient.Transcription(
                        "播放高山流水", new BigDecimal("0.9600"), "zh-CN"));
        when(voiceAiClient.analyzeIntent("播放高山流水")).thenReturn(Optional.empty());
        generatedIds();

        VoiceRecords.SongRow wrongSong = song(12L, "渔舟唱晚", 1);
        VoiceRecords.SongRow requestedSong = song(8L, "高山流水", 2);
        when(voiceMapper.findReadySongs()).thenReturn(List.of(wrongSong, requestedSong));
        when(songbookService.startPerformance(any())).thenReturn(
                new SongbookDtos.PerformanceResponse(
                        301L,
                        8L,
                        "高山流水",
                        "QUEUED",
                        LocalDateTime.of(2026, 8, 5, 12, 0),
                        null,
                        null));

        VoiceInteractionDtos.TranscriptionResponse response = service.transcribe(
                new MockMultipartFile(
                        "audioFile", "voice.webm", "audio/webm", new byte[]{1, 2, 3}),
                null,
                "zh-CN",
                1800);

        assertEquals("SONG_SEARCH", response.intentType());
        assertEquals(8L, response.match().songId());
        assertEquals(301L, response.performance().performanceId());
        assertEquals("QUEUED", response.performance().runStatus());
        assertFalse(response.match().showStartButton());
        verify(songbookService).startPerformance(any(SongbookDtos.PlayRequest.class));
    }

    @Test
    void qaUsesRetrievedKnowledgeAndReturnsReference() {
        generatedSessionAndUtteranceIds();
        VoiceRecords.KnowledgeRow history = new VoiceRecords.KnowledgeRow();
        history.setSourceType("INSTRUMENT_HISTORY");
        history.setSourceId(1L);
        history.setTitle("先秦起源");
        history.setContent("古筝雏形源于秦地。古筝的早期形态可追溯至先秦时期。");
        when(voiceMapper.findKnowledgeCandidates()).thenReturn(List.of(history));
        when(voiceAiClient.answer(any(), any())).thenReturn(Optional.of("古筝早期形态可追溯至先秦时期。"));
        when(voiceAiClient.modelName()).thenReturn("test-model");
        doAnswer(invocation -> {
            VoiceRecords.QaAnswerRow row = invocation.getArgument(0);
            row.setId(1201L);
            return 1;
        }).when(voiceMapper).insertQaAnswer(any());

        VoiceInteractionDtos.QaResponse response = service.answer(
                new VoiceInteractionDtos.QaRequest(
                        "古筝最早起源于哪个时期？",
                        null,
                        "TEXT",
                        null,
                        null,
                        5));

        assertEquals(1201L, response.requestId());
        assertEquals("COMPLETED", response.status());
        assertEquals("INSTRUMENT_HISTORY", response.references().get(0).sourceType());
        assertNotNull(response.conversationId());
    }

    @Test
    void unsupportedAudioFormatReturns415BeforeCallingAsr() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.transcribe(
                        new MockMultipartFile(
                                "audioFile", "voice.ogg", "audio/ogg", new byte[]{1}),
                        null,
                        "zh-CN",
                        100));

        assertEquals(415, exception.getCode());
    }

    private void generatedIds() {
        generatedSessionAndUtteranceIds();
        doAnswer(invocation -> {
            VoiceRecords.DigitalAssetRow row = invocation.getArgument(0);
            row.setId(901L);
            return 1;
        }).when(voiceMapper).insertAudioAsset(any());
    }

    private void generatedSessionAndUtteranceIds() {
        when(voiceMapper.findSessionIdByToken(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            VoiceRecords.SessionRow row = invocation.getArgument(0);
            row.setId(501L);
            return 1;
        }).when(voiceMapper).insertSession(any());
        doAnswer(invocation -> {
            VoiceRecords.UtteranceRow row = invocation.getArgument(0);
            row.setId(1002L);
            return 1;
        }).when(voiceMapper).insertUtterance(any());
    }

    private VoiceRecords.SongRow song(Long id, String title, int displayOrder) {
        VoiceRecords.SongRow row = new VoiceRecords.SongRow();
        row.setSongId(id);
        row.setTitle(title);
        row.setArtistName("古曲");
        row.setDisplayOrder(displayOrder);
        return row;
    }
}
