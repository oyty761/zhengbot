package com.guzheng.controller;

import com.guzheng.common.ApiResponse;
import com.guzheng.dto.*;
import com.guzheng.service.InstrumentExploreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instrument-explore")
@RequiredArgsConstructor
public class InstrumentExploreController {

    private final InstrumentExploreService instrumentExploreService;

    @GetMapping("/home")
    public ApiResponse<ExploreHomeDTO> home() {
        return ApiResponse.success(instrumentExploreService.getHomeModules());
    }

    @GetMapping("/structure/overview")
    public ApiResponse<StructureOverviewDTO> structureOverview() {
        return ApiResponse.success(instrumentExploreService.getStructureOverview());
    }

    @GetMapping("/structure/part-detail")
    public ApiResponse<PartDetailDTO> partDetail(@RequestParam("part_id") Long partId) {
        return ApiResponse.success(instrumentExploreService.getPartDetail(partId));
    }

    @GetMapping("/structure/string-highlight")
    public ApiResponse<StringHighlightDTO> stringHighlight(
            @RequestParam("string_no") Integer stringNo,
            @RequestParam("action") String action) {
        return ApiResponse.success(instrumentExploreService.highlightString(stringNo, action));
    }

    @GetMapping("/history/timeline")
    public ApiResponse<List<HistoryStageDTO>> historyTimeline() {
        return ApiResponse.success(instrumentExploreService.getHistoryTimeline());
    }

    @GetMapping("/history/stage-songs")
    public ApiResponse<StageSongsDTO> stageSongs(@RequestParam("stage_id") Long stageId) {
        return ApiResponse.success(instrumentExploreService.getStageSongs(stageId));
    }

    @PostMapping("/history/song-link")
    public ApiResponse<SongLinkDTO> songLink(
            @RequestParam("song_id") Long songId,
            @RequestParam("session_id") String sessionId) {
        return ApiResponse.success(instrumentExploreService.linkSongToSongbook(songId, sessionId));
    }
}
