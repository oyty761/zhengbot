package com.guzheng.voice.model;

import lombok.Data;

import java.math.BigDecimal;

/** 仅供语音模块内部及 MyBatis 使用的数据库记录。 */
public final class VoiceRecords {

    private VoiceRecords() {
    }

    @Data
    public static class SessionRow {
        private Long id;
        private String sessionToken;
    }

    @Data
    public static class DigitalAssetRow {
        private Long id;
        private String storageUri;
        private String mimeType;
        private String checksumSha256;
        private Integer durationMs;
    }

    @Data
    public static class UtteranceRow {
        private Long id;
        private Long sessionId;
        private String inputChannel;
        private String intentType;
        private String transcript;
        private Long audioAssetId;
        private BigDecimal asrConfidence;
        private Integer vadDurationMs;
    }

    @Data
    public static class SongRow {
        private Long songId;
        private String title;
        private String artistName;
        private String aliases;
        private String coverUrl;
        private Integer displayOrder;
    }

    @Data
    public static class KnowledgeRow {
        private String sourceType;
        private Long sourceId;
        private Long knowledgeItemId;
        private String title;
        private String content;
        private BigDecimal relevanceScore;
    }

    @Data
    public static class QaAnswerRow {
        private Long id;
        private Long utteranceId;
        private String answerText;
        private String modelName;
    }

    @Data
    public static class AnswerSourceRow {
        private Long answerId;
        private Long knowledgeItemId;
        private Integer rankNo;
        private BigDecimal relevanceScore;
    }
}
