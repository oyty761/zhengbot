package com.guzheng.songbook.service;

import com.guzheng.common.BusinessException;
import com.guzheng.songbook.dto.SongbookDtos;
import com.guzheng.songbook.mapper.SongbookMapper;
import com.guzheng.songbook.model.SongbookRecords;
import com.guzheng.songbook.service.impl.SongbookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SongbookServiceImplTest {

    @Mock
    private SongbookMapper songbookMapper;

    private SongbookServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SongbookServiceImpl(songbookMapper);
    }

    @Test
    void homeContainsLibraryAndRecommendationEntries() {
        SongbookDtos.HomeResponse response = service.getHome();

        assertEquals(2, response.modules().size());
        assertEquals("LIBRARY", response.modules().get(0).type());
        assertEquals("RECOMMEND", response.modules().get(1).type());
    }

    @Test
    void listSongsMapsDescriptorsAndPagination() {
        SongbookRecords.SongRow song = song(12L, "渔舟唱晚");
        song.setDescriptorNames("古风,舒缓,古风");
        when(songbookMapper.findPage("渔舟", 0, 12)).thenReturn(List.of(song));
        when(songbookMapper.countSongs("渔舟")).thenReturn(1L);

        SongbookDtos.PageResult<SongbookDtos.SongSummary> result =
                service.listSongs(1, 12, " 渔舟 ");

        assertEquals(1L, result.total());
        assertEquals(List.of("古风", "舒缓"), result.items().get(0).descriptors());
    }

    @Test
    void invalidPageSizeIsRejected() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.listSongs(1, 51, null));

        assertEquals(400, exception.getCode());
    }

    @Test
    void searchCreatesRequestAndCandidates() {
        SongbookRecords.SongRow song = song(8L, "高山流水");
        song.setMatchScore(BigDecimal.ONE);
        when(songbookMapper.searchSongs("高山流水", 6)).thenReturn(List.of(song));
        when(songbookMapper.completeDiscoveryRequest(102L, "COMPLETED")).thenReturn(1);
        doAnswer(invocation -> {
            SongbookRecords.UtteranceRow row = invocation.getArgument(0);
            row.setId(101L);
            return 1;
        }).when(songbookMapper).insertUtterance(any());
        doAnswer(invocation -> {
            SongbookRecords.DiscoveryRequestRow row = invocation.getArgument(0);
            row.setId(102L);
            return 1;
        }).when(songbookMapper).insertDiscoveryRequest(any());

        SongbookDtos.DiscoveryResponse response = service.search(discoveryInput("高山流水"));

        assertEquals(102L, response.requestId());
        assertEquals("SEARCH", response.requestKind());
        assertEquals("COMPLETED", response.status());
        assertEquals(8L, response.songs().get(0).songId());
        verify(songbookMapper).insertCandidates(anyList());
    }

    @Test
    void recommendationWithoutKnownDescriptorReturns422() {
        when(songbookMapper.findRecommendationDescriptors()).thenReturn(List.of());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.recommend(discoveryInput("随便来一首")));

        assertEquals(422, exception.getCode());
        verify(songbookMapper, never()).insertDiscoveryRequest(any());
    }

    @Test
    void startPerformanceCreatesDirectDiscoveryAndQueuedRun() {
        SongbookRecords.SongRow song = song(12L, "渔舟唱晚");
        when(songbookMapper.findSongDetail(12L)).thenReturn(song);
        when(songbookMapper.completeDiscoveryRequest(202L, "COMPLETED")).thenReturn(1);
        doAnswer(invocation -> {
            SongbookRecords.UtteranceRow row = invocation.getArgument(0);
            row.setId(201L);
            return 1;
        }).when(songbookMapper).insertUtterance(any());
        doAnswer(invocation -> {
            SongbookRecords.DiscoveryRequestRow row = invocation.getArgument(0);
            row.setId(202L);
            return 1;
        }).when(songbookMapper).insertDiscoveryRequest(any());
        doAnswer(invocation -> {
            SongbookRecords.PerformanceRow row = invocation.getArgument(0);
            row.setId(203L);
            return 1;
        }).when(songbookMapper).insertPerformance(any());

        SongbookRecords.PerformanceRow performance = new SongbookRecords.PerformanceRow();
        performance.setId(203L);
        performance.setWorkId(12L);
        performance.setSongTitle("渔舟唱晚");
        performance.setRunStatus("QUEUED");
        performance.setRequestedAt(LocalDateTime.now());
        when(songbookMapper.findPerformance(203L)).thenReturn(performance);

        SongbookDtos.PerformanceResponse response =
                service.startPerformance(new SongbookDtos.PlayRequest(12L, null));

        assertEquals(203L, response.performanceId());
        assertEquals("QUEUED", response.runStatus());
        verify(songbookMapper).markCandidateSelectedByRequest(202L, 12L);
    }

    @Test
    void duplicateFeedbackReturnsConflict() {
        SongbookRecords.PerformanceRow performance = new SongbookRecords.PerformanceRow();
        performance.setId(301L);
        performance.setRunStatus("SUCCEEDED");
        when(songbookMapper.findPerformance(301L)).thenReturn(performance);
        when(songbookMapper.findFeedbackIdByPerformance(301L)).thenReturn(401L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.submitFeedback(
                        301L,
                        new SongbookDtos.FeedbackRequest(5, "很好", List.of(1L))));

        assertEquals(409, exception.getCode());
        assertTrue(exception.getMessage().contains("已经提交"));
    }

    private SongbookDtos.DiscoveryInput discoveryInput(String content) {
        return new SongbookDtos.DiscoveryInput(
                content,
                null,
                "TEXT",
                null,
                null,
                null,
                null);
    }

    private SongbookRecords.SongRow song(Long id, String title) {
        SongbookRecords.SongRow row = new SongbookRecords.SongRow();
        row.setSongId(id);
        row.setTitle(title);
        row.setArtistName("测试作者");
        row.setOriginPeriod("近现代");
        return row;
    }
}

