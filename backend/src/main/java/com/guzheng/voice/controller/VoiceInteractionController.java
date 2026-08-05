package com.guzheng.voice.controller;

import com.guzheng.common.ApiResponse;
import com.guzheng.voice.dto.VoiceInteractionDtos;
import com.guzheng.voice.service.VoiceInteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/voice-interaction")
@RequiredArgsConstructor
public class VoiceInteractionController {

    private final VoiceInteractionService voiceInteractionService;

    @PostMapping(value = "/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<VoiceInteractionDtos.TranscriptionResponse> transcribe(
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam(required = false) String sessionToken,
            @RequestParam(defaultValue = "zh-CN") String language,
            @RequestParam(required = false) Integer vadDurationMs) {
        return ApiResponse.success(voiceInteractionService.transcribe(
                audioFile,
                sessionToken,
                language,
                vadDurationMs));
    }

    @PostMapping("/qa")
    public ApiResponse<VoiceInteractionDtos.QaResponse> answer(
            @Valid @RequestBody VoiceInteractionDtos.QaRequest request) {
        return ApiResponse.success(voiceInteractionService.answer(request));
    }
}
