package com.guzheng.explore.songbook.controller;

import com.guzheng.explore.common.ApiResponse;
import com.guzheng.explore.songbook.dto.SongbookDtos;
import com.guzheng.explore.songbook.service.SongbookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/songbook")
@RequiredArgsConstructor
public class SongbookController {

    private final SongbookService songbookService;

    @GetMapping("/home")
    public ApiResponse<SongbookDtos.HomeResponse> home() {
        return ApiResponse.success(songbookService.getHome());
    }

    @GetMapping("/songs")
    public ApiResponse<SongbookDtos.PageResult<SongbookDtos.SongSummary>> songs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(songbookService.listSongs(page, size, keyword));
    }

    @GetMapping("/songs/{songId}")
    public ApiResponse<SongbookDtos.SongDetail> songDetail(@PathVariable Long songId) {
        return ApiResponse.success(songbookService.getSongDetail(songId));
    }

    @PostMapping("/search")
    public ApiResponse<SongbookDtos.DiscoveryResponse> search(
            @Valid @RequestBody SongbookDtos.DiscoveryInput input) {
        return ApiResponse.success(songbookService.search(input));
    }

    @PostMapping("/recommendations")
    public ApiResponse<SongbookDtos.DiscoveryResponse> recommendations(
            @Valid @RequestBody SongbookDtos.DiscoveryInput input) {
        return ApiResponse.success(songbookService.recommend(input));
    }

    @PostMapping("/alternatives")
    public ApiResponse<SongbookDtos.DiscoveryResponse> alternatives(
            @Valid @RequestBody SongbookDtos.DiscoveryInput input) {
        return ApiResponse.success(songbookService.alternatives(input));
    }

    @PostMapping("/performances")
    public ApiResponse<SongbookDtos.PerformanceResponse> startPerformance(
            @Valid @RequestBody SongbookDtos.PlayRequest request) {
        return ApiResponse.success(songbookService.startPerformance(request));
    }

    @GetMapping("/performances/{performanceId}")
    public ApiResponse<SongbookDtos.PerformanceResponse> performance(
            @PathVariable Long performanceId) {
        return ApiResponse.success(songbookService.getPerformance(performanceId));
    }

    @GetMapping("/feedback/descriptors")
    public ApiResponse<List<SongbookDtos.DescriptorOption>> feedbackDescriptors() {
        return ApiResponse.success(songbookService.listFeedbackDescriptors());
    }

    @PostMapping("/performances/{performanceId}/feedback")
    public ApiResponse<SongbookDtos.FeedbackResponse> submitFeedback(
            @PathVariable Long performanceId,
            @Valid @RequestBody SongbookDtos.FeedbackRequest request) {
        return ApiResponse.success(songbookService.submitFeedback(performanceId, request));
    }
}
