package com.guzheng.voice.service;

import com.guzheng.voice.dto.VoiceInteractionDtos;
import org.springframework.web.multipart.MultipartFile;

public interface VoiceInteractionService {

    VoiceInteractionDtos.TranscriptionResponse transcribe(
            MultipartFile audioFile,
            String sessionToken,
            String language,
            Integer vadDurationMs);

    VoiceInteractionDtos.QaResponse answer(VoiceInteractionDtos.QaRequest request);
}
