package com.guzheng.composition.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** API objects for composition editing and robot-performance staging. */
public final class CompositionDtos {
    private CompositionDtos() {}

    public record NoteInput(
            @JsonAlias("note_id") Long noteId,
            @NotNull @Positive @JsonAlias({"composition_id"}) Long compositionId,
            @NotNull @Min(1) @Max(21) @JsonAlias({"string_no", "string_part_id"}) Integer stringNo,
            @NotNull @PositiveOrZero @JsonAlias("start_tick") Integer startTick,
            @NotNull @Positive @JsonAlias("duration_tick") Integer durationTick,
            @Min(1) @Max(127) Integer velocity,
            String technique,
            String hand) {}

    public record ScoreNote(
            @JsonAlias("note_id") Long noteId,
            @NotNull @Min(1) @Max(21) @JsonAlias({"string_no", "string_part_id"}) Integer stringNo,
            @NotNull @PositiveOrZero @JsonAlias("start_tick") Integer startTick,
            @NotNull @Positive @JsonAlias("duration_tick") Integer durationTick,
            @Min(1) @Max(127) Integer velocity,
            String technique,
            String hand) {}

    public record NoteView(Long noteId, Integer stringNo, Integer startTick,
                           Integer durationTick, Integer velocity, String technique,
                           String hand, Integer aiCompletionNo, String noteState) {}

    public record InitResponse(Long compositionId, Integer ticksPerBeat,
                               String editStatus, String title, Integer tempo,
                               String meter, List<NoteView> notes) {}

    public record NoteResponse(Long noteId, String message) {}

    public record ScoreInput(
            @NotBlank @Size(max=120) String title,
            @NotNull @Min(20) @Max(300) Integer tempo,
            @NotBlank String meter,
            @NotNull @Min(24) @Max(9600) Integer ticksPerBeat,
            @NotEmpty List<@Valid ScoreNote> notes) {}

    public record ScoreResponse(Long compositionId, int noteCount, String message) {}

    public record CompositionRequest(
            @NotNull @Positive @JsonAlias("composition_id") Long compositionId) {}

    public record AiCompletionResponse(Long completionId, List<NoteView> suggestedNotes) {}

    public record AiDecisionRequest(
            @NotNull @Positive @JsonAlias("composition_id") Long compositionId,
            @NotBlank String action,
            @NotNull @PositiveOrZero @JsonAlias("note_index") Integer noteIndex) {}

    public record MessageResponse(String message) {}

    public record CompleteResponse(Long compositionId, String editStatus,
                                   Long commandAssetId, String message) {}

    public record PerformanceRequest(
            @NotNull @Positive @JsonAlias("composition_id") Long compositionId,
            @NotNull @Positive @JsonAlias("command_asset_id") Long commandAssetId) {}

    public record PerformanceResponse(Long runId, String runStatus,
                                      LocalDateTime requestedAt,
                                      LocalDateTime startedAt,
                                      LocalDateTime endedAt,
                                      Long actualDurationMs,
                                      String message) {}
}
