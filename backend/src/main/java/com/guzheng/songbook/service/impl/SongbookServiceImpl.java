package com.guzheng.songbook.service.impl;

import com.guzheng.common.BusinessException;
import com.guzheng.songbook.dto.SongbookDtos;
import com.guzheng.songbook.mapper.SongbookMapper;
import com.guzheng.songbook.model.SongbookRecords;
import com.guzheng.songbook.service.SongbookService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class SongbookServiceImpl implements SongbookService {

    private static final int DEFAULT_LIMIT = 6;
    private static final int MAX_PAGE_SIZE = 50;
    private static final BigDecimal SEARCH_THRESHOLD = new BigDecimal("0.3000");

    private static final String INPUT_TEXT = "TEXT";
    private static final String INPUT_VOICE = "VOICE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_NO_MATCH = "NO_MATCH";

    private final SongbookMapper songbookMapper;

    @Override
    public SongbookDtos.HomeResponse getHome() {
        return new SongbookDtos.HomeResponse(List.of(
                new SongbookDtos.ModuleItem(
                        "曲库点歌",
                        "LIBRARY",
                        "浏览、搜索本地可演奏曲目并发起演奏"),
                new SongbookDtos.ModuleItem(
                        "智能推荐",
                        "RECOMMEND",
                        "按风格、情绪和场景推荐曲目")
        ));
    }

    @Override
    public SongbookDtos.PageResult<SongbookDtos.SongSummary> listSongs(
            int page,
            int size,
            String keyword) {
        if (page < 1) {
            throw new BusinessException(400, "页码必须大于等于1");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(400, "每页数量必须在1到50之间");
        }

        String normalizedKeyword = normalizeOptionalText(keyword);
        int offset;
        try {
            offset = Math.multiplyExact(page - 1, size);
        } catch (ArithmeticException exception) {
            throw new BusinessException(400, "页码过大");
        }

        List<SongbookDtos.SongSummary> items = songbookMapper
                .findPage(normalizedKeyword, offset, size)
                .stream()
                .map(this::toSummary)
                .toList();
        long total = songbookMapper.countSongs(normalizedKeyword);
        return new SongbookDtos.PageResult<>(page, size, total, items);
    }

    @Override
    public SongbookDtos.SongDetail getSongDetail(Long songId) {
        SongbookRecords.SongRow row = requireSong(songId);
        return new SongbookDtos.SongDetail(
                row.getSongId(),
                row.getTitle(),
                row.getArtistName(),
                row.getOriginPeriod(),
                row.getBackgroundText(),
                row.getStyleText(),
                row.getFeaturedExcerpt(),
                row.getCoverUrl(),
                row.getPreviewUrl(),
                row.getScoreUrl(),
                splitDescriptors(row.getDescriptorNames()));
    }

    @Override
    @Transactional
    public SongbookDtos.DiscoveryResponse search(SongbookDtos.DiscoveryInput input) {
        int limit = resolveLimit(input.limit());
        Long sessionId = resolveSession(input.sessionToken());
        Long utteranceId = createUtterance(sessionId, input, "SONG_SEARCH");

        String query = input.content().trim();
        List<SongbookRecords.SongRow> matches = songbookMapper.searchSongs(query, limit);
        SongbookRecords.DiscoveryRequestRow searchRequest =
                createRequest(sessionId, utteranceId, "SEARCH", SEARCH_THRESHOLD);

        if (!matches.isEmpty()) {
            saveCandidates(searchRequest.getId(), matches, "MATCH");
            completeRequest(searchRequest.getId(), STATUS_COMPLETED);
            return discoveryResponse(
                    searchRequest.getId(),
                    "SEARCH",
                    STATUS_COMPLETED,
                    "已找到可演奏曲目",
                    matches);
        }

        completeRequest(searchRequest.getId(), STATUS_NO_MATCH);

        List<SongbookRecords.SongRow> substitutes = findRecommendations(query, limit);
        SongbookRecords.DiscoveryRequestRow alternativeRequest =
                createRequest(sessionId, utteranceId, "ALTERNATIVE", SEARCH_THRESHOLD);
        saveCandidates(alternativeRequest.getId(), substitutes, "SUBSTITUTE");
        String status = substitutes.isEmpty() ? STATUS_NO_MATCH : STATUS_COMPLETED;
        completeRequest(alternativeRequest.getId(), status);
        return discoveryResponse(
                alternativeRequest.getId(),
                "ALTERNATIVE",
                status,
                substitutes.isEmpty()
                        ? "当前曲库暂无可替代曲目"
                        : "当前曲库暂不支持该歌曲，为你推荐以下相近曲目",
                substitutes);
    }

    @Override
    @Transactional
    public SongbookDtos.DiscoveryResponse recommend(SongbookDtos.DiscoveryInput input) {
        int limit = resolveLimit(input.limit());
        List<Long> descriptorIds = matchDescriptorIds(input.content());
        if (descriptorIds.isEmpty()) {
            throw new BusinessException(422, "没有识别到风格、情绪或场景，请换一种说法");
        }

        Long sessionId = resolveSession(input.sessionToken());
        Long utteranceId = createUtterance(sessionId, input, "RECOMMENDATION");
        List<SongbookRecords.SongRow> songs =
                songbookMapper.recommendSongs(descriptorIds, descriptorIds.size(), limit);
        SongbookRecords.DiscoveryRequestRow request =
                createRequest(sessionId, utteranceId, "RECOMMEND", null);
        saveCandidates(request.getId(), songs, "RECOMMENDED");

        String status = songs.isEmpty() ? STATUS_NO_MATCH : STATUS_COMPLETED;
        completeRequest(request.getId(), status);
        return discoveryResponse(
                request.getId(),
                "RECOMMEND",
                status,
                songs.isEmpty() ? "当前曲库没有匹配曲目" : "已根据你的需求生成推荐列表",
                songs);
    }

    @Override
    @Transactional
    public SongbookDtos.DiscoveryResponse alternatives(SongbookDtos.DiscoveryInput input) {
        int limit = resolveLimit(input.limit());
        Long sessionId = resolveSession(input.sessionToken());
        Long utteranceId = createUtterance(sessionId, input, "SONG_SEARCH");
        List<SongbookRecords.SongRow> songs = findRecommendations(input.content(), limit);

        SongbookRecords.DiscoveryRequestRow request =
                createRequest(sessionId, utteranceId, "ALTERNATIVE", SEARCH_THRESHOLD);
        saveCandidates(request.getId(), songs, "SUBSTITUTE");
        String status = songs.isEmpty() ? STATUS_NO_MATCH : STATUS_COMPLETED;
        completeRequest(request.getId(), status);
        return discoveryResponse(
                request.getId(),
                "ALTERNATIVE",
                status,
                songs.isEmpty() ? "当前曲库暂无可替代曲目" : "已为你推荐相近曲目",
                songs);
    }

    @Override
    @Transactional
    public SongbookDtos.PerformanceResponse startPerformance(SongbookDtos.PlayRequest request) {
        SongbookRecords.SongRow song = requireSong(request.songId());
        Long sessionId = resolveSession(request.sessionToken());

        SongbookRecords.UtteranceRow utterance = new SongbookRecords.UtteranceRow();
        utterance.setSessionId(sessionId);
        utterance.setInputChannel(INPUT_TEXT);
        utterance.setIntentType("SONG_SEARCH");
        utterance.setTranscript(song.getTitle());
        songbookMapper.insertUtterance(utterance);

        SongbookRecords.DiscoveryRequestRow discoveryRequest =
                createRequest(sessionId, utterance.getId(), "DIRECT", null);

        SongbookRecords.DiscoveryCandidateRow candidate =
                new SongbookRecords.DiscoveryCandidateRow();
        candidate.setRequestId(discoveryRequest.getId());
        candidate.setSongId(song.getSongId());
        candidate.setRankNo(1);
        candidate.setMatchScore(BigDecimal.ONE.setScale(4, RoundingMode.UNNECESSARY));
        candidate.setCandidateRole("MATCH");
        songbookMapper.insertCandidates(List.of(candidate));
        songbookMapper.markCandidateSelectedByRequest(discoveryRequest.getId(), song.getSongId());
        completeRequest(discoveryRequest.getId(), STATUS_COMPLETED);

        SongbookRecords.PerformanceRow performance = new SongbookRecords.PerformanceRow();
        performance.setSessionId(sessionId);
        performance.setWorkId(song.getSongId());
        performance.setOriginModule("SONGBOOK");
        performance.setRunStatus("QUEUED");
        songbookMapper.insertPerformance(performance);
        return getPerformance(performance.getId());
    }

    @Override
    public SongbookDtos.PerformanceResponse getPerformance(Long performanceId) {
        if (performanceId == null || performanceId <= 0) {
            throw new BusinessException(400, "演奏任务ID不合法");
        }
        SongbookRecords.PerformanceRow performance =
                songbookMapper.findPerformance(performanceId);
        if (performance == null) {
            throw new BusinessException(404, "演奏任务不存在");
        }
        return new SongbookDtos.PerformanceResponse(
                performance.getId(),
                performance.getWorkId(),
                performance.getSongTitle(),
                performance.getRunStatus(),
                performance.getRequestedAt(),
                performance.getStartedAt(),
                performance.getEndedAt());
    }

    @Override
    public List<SongbookDtos.DescriptorOption> listFeedbackDescriptors() {
        return songbookMapper.findFeedbackDescriptors()
                .stream()
                .map(item -> new SongbookDtos.DescriptorOption(
                        item.getId(),
                        item.getDescriptorType(),
                        item.getName()))
                .toList();
    }

    @Override
    @Transactional
    public SongbookDtos.FeedbackResponse submitFeedback(
            Long performanceId,
            SongbookDtos.FeedbackRequest request) {
        SongbookRecords.PerformanceRow performance =
                songbookMapper.findPerformance(performanceId);
        if (performance == null) {
            throw new BusinessException(404, "演奏任务不存在");
        }
        if (!"SUCCEEDED".equals(performance.getRunStatus())) {
            throw new BusinessException(409, "只有演奏完成后才能提交反馈");
        }
        if (songbookMapper.findFeedbackIdByPerformance(performanceId) != null) {
            throw new BusinessException(409, "该演奏任务已经提交过反馈");
        }

        List<Long> descriptorIds = request.descriptorIds().stream().distinct().toList();
        if (descriptorIds.isEmpty()
                || songbookMapper.countFeedbackDescriptors(descriptorIds) != descriptorIds.size()) {
            throw new BusinessException(400, "包含无效或不可用的反馈标签");
        }

        SongbookRecords.FeedbackRow feedback = new SongbookRecords.FeedbackRow();
        feedback.setPerformanceRunId(performanceId);
        feedback.setRating(request.rating());
        feedback.setCommentText(normalizeOptionalText(request.comment()));
        try {
            songbookMapper.insertFeedback(feedback);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "该演奏任务已经提交过反馈");
        }
        songbookMapper.insertFeedbackDescriptors(feedback.getId(), descriptorIds);
        return new SongbookDtos.FeedbackResponse(
                feedback.getId(),
                performanceId,
                "反馈已记录");
    }

    private Long resolveSession(String sessionToken) {
        if (!StringUtils.hasText(sessionToken)) {
            return null;
        }

        String normalized = sessionToken.trim();
        try {
            UUID.fromString(normalized);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "sessionToken 必须是标准 UUID");
        }

        Long sessionId = songbookMapper.findSessionIdByToken(normalized);
        if (sessionId != null) {
            return sessionId;
        }

        SongbookRecords.SessionRow session = new SongbookRecords.SessionRow();
        session.setSessionToken(normalized);
        try {
            songbookMapper.insertSession(session);
            return session.getId();
        } catch (DuplicateKeyException exception) {
            Long existingId = songbookMapper.findSessionIdByToken(normalized);
            if (existingId != null) {
                return existingId;
            }
            throw exception;
        }
    }

    private Long createUtterance(
            Long sessionId,
            SongbookDtos.DiscoveryInput input,
            String intentType) {
        String inputChannel = StringUtils.hasText(input.inputChannel())
                ? input.inputChannel().trim().toUpperCase(Locale.ROOT)
                : INPUT_TEXT;
        if (!Set.of(INPUT_TEXT, INPUT_VOICE).contains(inputChannel)) {
            throw new BusinessException(400, "inputChannel 仅支持 TEXT 或 VOICE");
        }
        if (INPUT_VOICE.equals(inputChannel) && input.audioAssetId() == null) {
            throw new BusinessException(400, "语音输入必须提供 audioAssetId");
        }
        if (INPUT_TEXT.equals(inputChannel)
                && (input.audioAssetId() != null
                || input.asrConfidence() != null
                || input.vadDurationMs() != null)) {
            throw new BusinessException(400, "文字输入不能携带语音字段");
        }
        if (input.audioAssetId() != null
                && songbookMapper.countDigitalAsset(input.audioAssetId()) == 0) {
            throw new BusinessException(400, "audioAssetId 对应的语音资源不存在");
        }

        SongbookRecords.UtteranceRow utterance = new SongbookRecords.UtteranceRow();
        utterance.setSessionId(sessionId);
        utterance.setInputChannel(inputChannel);
        utterance.setIntentType(intentType);
        utterance.setTranscript(input.content().trim());
        utterance.setAudioAssetId(input.audioAssetId());
        utterance.setAsrConfidence(input.asrConfidence());
        utterance.setVadDurationMs(input.vadDurationMs());
        songbookMapper.insertUtterance(utterance);
        return utterance.getId();
    }

    private SongbookRecords.DiscoveryRequestRow createRequest(
            Long sessionId,
            Long utteranceId,
            String requestKind,
            BigDecimal minMatchScore) {
        SongbookRecords.DiscoveryRequestRow request =
                new SongbookRecords.DiscoveryRequestRow();
        request.setSessionId(sessionId);
        request.setUtteranceId(utteranceId);
        request.setRequestKind(requestKind);
        request.setStatus(STATUS_PENDING);
        request.setMinMatchScore(minMatchScore);
        songbookMapper.insertDiscoveryRequest(request);
        return request;
    }

    private void completeRequest(Long requestId, String status) {
        if (songbookMapper.completeDiscoveryRequest(requestId, status) != 1) {
            throw new BusinessException(500, "歌曲发现请求状态更新失败");
        }
    }

    private void saveCandidates(
            Long requestId,
            List<SongbookRecords.SongRow> songs,
            String role) {
        if (songs.isEmpty()) {
            return;
        }
        List<SongbookRecords.DiscoveryCandidateRow> candidates =
                IntStream.range(0, songs.size())
                        .mapToObj(index -> {
                            SongbookRecords.DiscoveryCandidateRow candidate =
                                    new SongbookRecords.DiscoveryCandidateRow();
                            candidate.setRequestId(requestId);
                            candidate.setSongId(songs.get(index).getSongId());
                            candidate.setRankNo(index + 1);
                            candidate.setMatchScore(normalizeScore(songs.get(index).getMatchScore()));
                            candidate.setCandidateRole(role);
                            return candidate;
                        })
                        .toList();
        songbookMapper.insertCandidates(candidates);
    }

    private List<SongbookRecords.SongRow> findRecommendations(String content, int limit) {
        List<Long> descriptorIds = matchDescriptorIds(content);
        if (!descriptorIds.isEmpty()) {
            return songbookMapper.recommendSongs(
                    descriptorIds,
                    descriptorIds.size(),
                    limit);
        }
        return songbookMapper.fallbackSongs(limit);
    }

    private List<Long> matchDescriptorIds(String content) {
        String normalized = content.trim().toLowerCase(Locale.ROOT);
        List<SongbookRecords.DescriptorRow> descriptors = songbookMapper
                .findRecommendationDescriptors()
                .stream()
                .filter(descriptor -> StringUtils.hasText(descriptor.getName()))
                .sorted(Comparator.comparingInt(
                                (SongbookRecords.DescriptorRow descriptor) -> descriptor.getName().length())
                        .reversed())
                .toList();

        List<String> selectedNames = new ArrayList<>();
        LinkedHashSet<Long> selectedIds = new LinkedHashSet<>();
        for (SongbookRecords.DescriptorRow descriptor : descriptors) {
            String descriptorName = descriptor.getName().trim().toLowerCase(Locale.ROOT);
            if (!normalized.contains(descriptorName)) {
                continue;
            }
            // 若已经命中更长的描述词，不再把它包含的短词重复计入推荐条件。
            boolean coveredByLongerMatch = selectedNames.stream()
                    .anyMatch(selectedName -> selectedName.contains(descriptorName));
            if (!coveredByLongerMatch) {
                selectedNames.add(descriptorName);
                selectedIds.add(descriptor.getId());
            }
        }
        return List.copyOf(selectedIds);
    }

    private SongbookRecords.SongRow requireSong(Long songId) {
        if (songId == null || songId <= 0) {
            throw new BusinessException(400, "歌曲ID不合法");
        }
        SongbookRecords.SongRow row = songbookMapper.findSongDetail(songId);
        if (row == null) {
            throw new BusinessException(404, "歌曲不存在或当前不可演奏");
        }
        return row;
    }

    private SongbookDtos.DiscoveryResponse discoveryResponse(
            Long requestId,
            String requestKind,
            String status,
            String message,
            List<SongbookRecords.SongRow> songs) {
        return new SongbookDtos.DiscoveryResponse(
                requestId,
                requestKind,
                status,
                message,
                songs.stream().map(this::toSummary).toList());
    }

    private SongbookDtos.SongSummary toSummary(SongbookRecords.SongRow row) {
        return new SongbookDtos.SongSummary(
                row.getSongId(),
                row.getTitle(),
                row.getArtistName(),
                row.getOriginPeriod(),
                row.getStyleText(),
                row.getCoverUrl(),
                row.getPreviewUrl(),
                splitDescriptors(row.getDescriptorNames()),
                normalizeScore(row.getMatchScore()));
    }

    private List<String> splitDescriptors(String descriptorNames) {
        if (!StringUtils.hasText(descriptorNames)) {
            return List.of();
        }
        return Arrays.stream(descriptorNames.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf));
    }

    private BigDecimal normalizeScore(BigDecimal score) {
        if (score == null) {
            return null;
        }
        if (score.signum() < 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.UNNECESSARY);
        }
        if (score.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE.setScale(4, RoundingMode.UNNECESSARY);
        }
        return score.setScale(4, RoundingMode.HALF_UP);
    }

    private int resolveLimit(Integer limit) {
        return limit == null ? DEFAULT_LIMIT : limit;
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
