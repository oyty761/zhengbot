package com.guzheng.composition.controller;

import com.guzheng.common.ApiResponse;
import com.guzheng.composition.dto.CompositionDtos;
import com.guzheng.composition.service.CompositionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/performance")
@CrossOrigin
@RequiredArgsConstructor
public class PerformanceController {
    private final CompositionService service;
    @PostMapping("/run") public ApiResponse<CompositionDtos.PerformanceResponse> start(@Valid @RequestBody CompositionDtos.PerformanceRequest request){return ApiResponse.success(service.startPerformance(request));}
    @GetMapping("/run/{runId}") public ApiResponse<CompositionDtos.PerformanceResponse> get(@PathVariable long runId){return ApiResponse.success(service.performance(runId));}
}
