package com.guzheng.explore.songbook.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MyBatis 使用的内部数据记录，不直接作为接口响应返回。
 */
public final class SongbookRecords {

    private SongbookRecords() {
    }

    @Data
    public static class SongRow {
        private Long songId;
        private String title;
        private String artistName;
        private String originPeriod;
        private String backgroundText;
        private String styleText;
        private String featuredExcerpt;
        private String coverUrl;
        private String previewUrl;
        private String scoreUrl;
        private String descriptorNames;
        private BigDecimal matchScore;
    }

    @Data
    public static class DescriptorRow {
        private Long id;
        private String descriptorType;
        private String name;
    }

    @Data
    public static class SessionRow {
        private Long id;
        private String sessionToken;
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
    public static class DiscoveryRequestRow {
        private Long id;
        private Long sessionId;
        private Long utteranceId;
        private String requestKind;
        private String status;
        private BigDecimal minMatchScore;
    }

    @Data
    public static class DiscoveryCandidateRow {
        private Long id;
        private Long requestId;
        private Long songId;
        private Integer rankNo;
        private BigDecimal matchScore;
        private String candidateRole;
    }

    @Data
    public static class PerformanceRow {
        private Long id;
        private Long sessionId;
        private Long workId;
        private String songTitle;
        private String originModule;
        private String runStatus;
        private LocalDateTime requestedAt;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
    }

    @Data
    public static class FeedbackRow {
        private Long id;
        private Long performanceRunId;
        private Integer rating;
        private String commentText;
    }
}
