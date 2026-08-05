package com.guzheng.voice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 语音交互模块的接口数据对象。 */
public final class VoiceInteractionDtos {

    private VoiceInteractionDtos() {
    }

    public record SongEntities(String songTitle, String artistName) {
    }

    public record SongMatch(
            boolean matched,
            BigDecimal matchThreshold,
            BigDecimal topScore,
            Long songId,
            String title,
            String artistName,
            String coverUrl,
            String displayMessage,
            boolean showStartButton) {
    }

    /** 明确点歌匹配成功后直接创建的机器人演奏任务。 */
    public record PerformanceCommand(
            Long performanceId,
            Long songId,
            String songTitle,
            String runStatus,
            LocalDateTime requestedAt) {
    }

    public record TranscriptionResponse(
            Long transcriptionId,
            String sessionToken,
            Long audioAssetId,
            String content,
            String language,
            BigDecimal asrConfidence,
            Integer vadDurationMs,
            String status,
            String intentType,
            BigDecimal intentConfidence,
            SongEntities entities,
            SongMatch match,
            String targetPath,
            boolean clarificationRequired,
            String promptMessage,
            PerformanceCommand performance) {
    }

    public record QaRequest(
            @NotBlank(message = "问题内容不能为空")
            @Size(max = 500, message = "问题内容不能超过500个字符")
            String content,
            String sessionToken,
            String inputChannel,
            @Positive(message = "transcriptionId 必须大于0")
            Long transcriptionId,
            @Positive(message = "conversationId 必须大于0")
            Long conversationId,
            @Min(value = 1, message = "topK 至少为1")
            @Max(value = 10, message = "topK 不能超过10")
            Integer topK) {
    }

    public record KnowledgeReference(String sourceType, Long sourceId, String title) {
    }

    public record QaResponse(
            Long requestId,
            Long conversationId,
            String status,
            String answer,
            List<KnowledgeReference> references) {
    }
}
