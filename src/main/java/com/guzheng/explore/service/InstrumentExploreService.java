package com.guzheng.explore.service;

import com.guzheng.explore.dto.*;

import java.util.List;

public interface InstrumentExploreService {

    ExploreHomeDTO getHomeModules();

    StructureOverviewDTO getStructureOverview();

    PartDetailDTO getPartDetail(Long partId);

    StringHighlightDTO highlightString(Integer stringNo, String action);

    List<HistoryStageDTO> getHistoryTimeline();

    StageSongsDTO getStageSongs(Long stageId);

    SongLinkDTO linkSongToSongbook(Long songId, String sessionId);
}
