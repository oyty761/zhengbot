package com.guzheng.service;

import com.guzheng.dto.*;

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
