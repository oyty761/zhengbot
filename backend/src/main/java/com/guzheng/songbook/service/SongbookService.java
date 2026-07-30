package com.guzheng.songbook.service;

import com.guzheng.songbook.dto.SongbookDtos;

import java.util.List;

public interface SongbookService {

    SongbookDtos.HomeResponse getHome();

    SongbookDtos.PageResult<SongbookDtos.SongSummary> listSongs(int page, int size, String keyword);

    SongbookDtos.SongDetail getSongDetail(Long songId);

    SongbookDtos.DiscoveryResponse search(SongbookDtos.DiscoveryInput input);

    SongbookDtos.DiscoveryResponse recommend(SongbookDtos.DiscoveryInput input);

    SongbookDtos.DiscoveryResponse alternatives(SongbookDtos.DiscoveryInput input);

    SongbookDtos.PerformanceResponse startPerformance(SongbookDtos.PlayRequest request);

    SongbookDtos.PerformanceResponse getPerformance(Long performanceId);

    List<SongbookDtos.DescriptorOption> listFeedbackDescriptors();

    SongbookDtos.FeedbackResponse submitFeedback(
            Long performanceId,
            SongbookDtos.FeedbackRequest request);
}

