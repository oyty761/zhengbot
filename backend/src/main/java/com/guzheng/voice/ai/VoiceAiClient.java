package com.guzheng.voice.ai;

import com.guzheng.voice.model.VoiceRecords;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** 外部 ASR/大模型能力边界，便于替换不同供应商并在测试中隔离网络调用。 */
public interface VoiceAiClient {

    Transcription transcribe(byte[] audio, String fileName, String mimeType, String language);

    Optional<IntentAnalysis> analyzeIntent(String content);

    Optional<String> answer(String question, List<VoiceRecords.KnowledgeRow> context);

    String modelName();

    record Transcription(String content, BigDecimal confidence, String language) {
    }

    record IntentAnalysis(
            String intentType,
            BigDecimal confidence,
            String songTitle,
            String artistName) {
    }
}
