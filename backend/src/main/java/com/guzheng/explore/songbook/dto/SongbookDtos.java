package com.guzheng.explore.songbook.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * “我要点歌”模块的接口数据对象。
 *
 * <p>字段命名与 docs/API/我要点歌板块接口.md 保持一致，统一使用 Java/Jackson
 * 默认的 camelCase JSON 命名。</p>
 */
public final class SongbookDtos {

    private SongbookDtos() {
    }

    public record ModuleItem(String name, String type, String description) {
    }

    public record HomeResponse(List<ModuleItem> modules) {
    }

    public record SongSummary(
            Long songId,
            String title,
            String artistName,
            String originPeriod,
            String styleText,
            String coverUrl,
            String previewUrl,
            List<String> descriptors,
            BigDecimal matchScore) {
    }

    public record SongDetail(
            Long songId,
            String title,
            String artistName,
            String originPeriod,
            String backgroundText,
            String styleText,
            String featuredExcerpt,
            String coverUrl,
            String previewUrl,
            String scoreUrl,
            List<String> descriptors) {
    }

    public record PageResult<T>(int page, int size, long total, List<T> items) {
    }

    public record DiscoveryInput(
            @NotBlank(message = "点歌需求不能为空")
            @Size(max = 500, message = "点歌需求不能超过500个字符")
            String content,
            String sessionToken,
            String inputChannel,
            @Positive(message = "audioAssetId 必须大于0")
            Long audioAssetId,
            @DecimalMin(value = "0.0", message = "asrConfidence 不能小于0")
            @DecimalMax(value = "1.0", message = "asrConfidence 不能大于1")
            BigDecimal asrConfidence,
            @PositiveOrZero(message = "vadDurationMs 不能小于0")
            Integer vadDurationMs,
            @Min(value = 1, message = "返回数量至少为1")
            @Max(value = 20, message = "返回数量不能超过20")
            Integer limit) {
    }

    public record DiscoveryResponse(
            Long requestId,
            String requestKind,
            String status,
            String message,
            List<SongSummary> songs) {
    }

    public record PlayRequest(
            @NotNull(message = "歌曲ID不能为空")
            @Positive(message = "歌曲ID必须大于0")
            Long songId,
            String sessionToken) {
    }

    public record PerformanceResponse(
            Long performanceId,
            Long songId,
            String songTitle,
            String runStatus,
            LocalDateTime requestedAt,
            LocalDateTime startedAt,
            LocalDateTime endedAt) {
    }

    public record DescriptorOption(Long descriptorId, String type, String name) {
    }

    public record FeedbackRequest(
            @Min(value = 1, message = "评分最低为1")
            @Max(value = 5, message = "评分最高为5")
            Integer rating,
            @Size(max = 1000, message = "评价不能超过1000个字符")
            String comment,
            @NotEmpty(message = "请至少选择一个感受标签")
            List<@NotNull(message = "感受标签ID不能为空")
                 @Positive(message = "感受标签ID必须大于0") Long> descriptorIds) {
    }

    public record FeedbackResponse(Long feedbackId, Long performanceId, String message) {
    }
}
