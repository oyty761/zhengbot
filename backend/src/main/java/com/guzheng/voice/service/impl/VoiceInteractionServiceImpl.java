package com.guzheng.voice.service.impl;

import com.guzheng.common.BusinessException;
import com.guzheng.songbook.dto.SongbookDtos;
import com.guzheng.songbook.service.SongbookService;
import com.guzheng.voice.ai.VoiceAiClient;
import com.guzheng.voice.config.VoiceApiProperties;
import com.guzheng.voice.dto.VoiceInteractionDtos;
import com.guzheng.voice.mapper.VoiceInteractionMapper;
import com.guzheng.voice.model.VoiceRecords;
import com.guzheng.voice.service.VoiceInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class VoiceInteractionServiceImpl implements VoiceInteractionService {

    private static final long MAX_AUDIO_BYTES = 10L * 1024 * 1024;
    private static final int DEFAULT_TOP_K = 5;
    private static final BigDecimal MATCH_THRESHOLD = new BigDecimal("0.3000");
    private static final BigDecimal ZERO_SCORE = new BigDecimal("0.0000");
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("wav", "mp3", "m4a", "webm");
    private static final Pattern BOOK_TITLE = Pattern.compile("《([^》]{1,100})》");
    private static final Pattern ARTIST_PATTERN = Pattern.compile(
            "(?:歌手|作者|作曲|演奏者)(?:是|为|叫)?([\\p{IsHan}A-Za-z0-9·.]{1,40})");
    private static final Pattern NON_TEXT = Pattern.compile("[^\\p{IsHan}A-Za-z0-9]");

    private static final List<String> RECOMMEND_WORDS = List.of(
            "舒缓", "轻松", "欢快", "古风", "民族", "典雅", "治愈", "安静", "热烈", "悲伤", "场景", "风格");
    private static final List<String> QA_WORDS = List.of(
            "什么", "为什么", "怎么", "如何", "哪一", "多少", "起源", "历史", "结构", "部件", "琴弦", "琴码", "面板", "技法", "背景", "介绍", "讲讲");
    private static final List<String> SONG_COMMAND_WORDS = List.of(
            "播放", "演奏", "点播", "点歌", "我想听", "我要听", "来一首", "放一首", "听一首");

    private final VoiceInteractionMapper voiceMapper;
    private final VoiceAiClient voiceAiClient;
    private final SongbookService songbookService;
    private final VoiceApiProperties properties;

    @Override
    @Transactional
    public VoiceInteractionDtos.TranscriptionResponse transcribe(
            MultipartFile audioFile,
            String sessionToken,
            String language,
            Integer vadDurationMs) {
        AudioInput audio = validateAndReadAudio(audioFile, language, vadDurationMs);
        VoiceAiClient.Transcription transcription = voiceAiClient.transcribe(
                audio.bytes(),
                audio.fileName(),
                audio.mimeType(),
                audio.language());
        if (!StringUtils.hasText(transcription.content())) {
            throw new BusinessException(422, "未检测到有效语音");
        }

        String content = transcription.content().trim();
        Session session = resolveSession(sessionToken);
        VoiceRecords.DigitalAssetRow asset = saveAudio(audio, vadDurationMs);
        ResolvedIntent intent = resolveIntent(content);

        VoiceRecords.UtteranceRow utterance = new VoiceRecords.UtteranceRow();
        utterance.setSessionId(session.id());
        utterance.setInputChannel("VOICE");
        utterance.setIntentType(toDatabaseIntent(intent.intentType()));
        utterance.setTranscript(content);
        utterance.setAudioAssetId(asset.getId());
        utterance.setAsrConfidence(scaleConfidence(transcription.confidence()));
        utterance.setVadDurationMs(vadDurationMs);
        voiceMapper.insertUtterance(utterance);

        VoiceInteractionDtos.SongEntities entities = null;
        VoiceInteractionDtos.SongMatch match = null;
        VoiceInteractionDtos.PerformanceCommand performance = null;
        String targetPath = null;
        boolean clarificationRequired = false;
        String promptMessage = null;

        if ("SONG_SEARCH".equals(intent.intentType())) {
            entities = new VoiceInteractionDtos.SongEntities(intent.songTitle(), intent.artistName());
            MatchedSong matchedSong = matchSong(intent.songTitle(), intent.artistName());
            if (matchedSong.matched()) {
                SongbookDtos.PerformanceResponse task = songbookService.startPerformance(
                        new SongbookDtos.PlayRequest(matchedSong.song().getSongId(), session.token()));
                performance = new VoiceInteractionDtos.PerformanceCommand(
                        task.performanceId(),
                        task.songId(),
                        task.songTitle(),
                        task.runStatus(),
                        task.requestedAt());
                targetPath = "/api/songbook/performances/" + task.performanceId();
                match = toMatch(matchedSong, "已找到可演奏曲目，演奏任务已创建", false);
            } else {
                match = toMatch(matchedSong, "暂不支持该曲目", false);
            }
        } else if ("SONG_RECOMMEND".equals(intent.intentType())) {
            targetPath = "/api/songbook/recommendations";
        } else if ("KNOWLEDGE_QA".equals(intent.intentType())) {
            targetPath = "/api/voice-interaction/qa";
        } else {
            clarificationRequired = true;
            promptMessage = "没有听清你的需求，请说出古筝问题或想听的具体曲目";
        }

        return new VoiceInteractionDtos.TranscriptionResponse(
                utterance.getId(),
                session.token(),
                asset.getId(),
                content,
                StringUtils.hasText(transcription.language())
                        ? transcription.language()
                        : audio.language(),
                scaleConfidence(transcription.confidence()),
                vadDurationMs,
                "COMPLETED",
                intent.intentType(),
                scaleConfidence(intent.confidence()),
                entities,
                match,
                targetPath,
                clarificationRequired,
                promptMessage,
                performance);
    }

    @Override
    @Transactional
    public VoiceInteractionDtos.QaResponse answer(VoiceInteractionDtos.QaRequest request) {
        String channel = normalizeChannel(request.inputChannel());
        VoiceRecords.UtteranceRow utterance;
        Session session;

        if (request.transcriptionId() != null) {
            utterance = voiceMapper.findUtterance(request.transcriptionId());
            if (utterance == null) {
                throw new BusinessException(404, "语音转写记录不存在");
            }
            if (utterance.getSessionId() != null) {
                session = new Session(utterance.getSessionId(), normalizeOrGenerateToken(request.sessionToken()));
            } else {
                session = resolveSession(request.sessionToken());
            }
        } else {
            if ("VOICE".equals(channel)) {
                throw new BusinessException(400, "语音问答必须提供 transcriptionId");
            }
            session = resolveSession(request.sessionToken());
            utterance = new VoiceRecords.UtteranceRow();
            utterance.setSessionId(session.id());
            utterance.setInputChannel("TEXT");
            utterance.setIntentType("QA");
            utterance.setTranscript(request.content().trim());
            voiceMapper.insertUtterance(utterance);
        }

        List<VoiceRecords.KnowledgeRow> references = retrieveKnowledge(
                request.content(),
                request.topK() == null ? DEFAULT_TOP_K : request.topK());
        String generatedAnswer = voiceAiClient.answer(request.content().trim(), references)
                .orElseGet(() -> fallbackAnswer(references));

        VoiceRecords.QaAnswerRow answer = new VoiceRecords.QaAnswerRow();
        answer.setUtteranceId(utterance.getId());
        answer.setAnswerText(generatedAnswer);
        answer.setModelName(voiceAiClient.modelName());
        try {
            voiceMapper.insertQaAnswer(answer);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "该问题已经生成过回答");
        }
        saveAnswerSources(answer.getId(), references);

        Long conversationId = request.conversationId() != null
                ? request.conversationId()
                : session.id();
        return new VoiceInteractionDtos.QaResponse(
                answer.getId(),
                conversationId,
                "COMPLETED",
                generatedAnswer,
                references.stream()
                        .map(item -> new VoiceInteractionDtos.KnowledgeReference(
                                item.getSourceType(),
                                item.getSourceId(),
                                item.getTitle()))
                        .toList());
    }

    private AudioInput validateAndReadAudio(
            MultipartFile audioFile,
            String language,
            Integer vadDurationMs) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(422, "录音文件为空或未检测到有效语音");
        }
        if (audioFile.getSize() > MAX_AUDIO_BYTES) {
            throw new BusinessException(413, "录音文件不能超过10 MB");
        }
        if (vadDurationMs != null && vadDurationMs < 0) {
            throw new BusinessException(400, "vadDurationMs 不能小于0");
        }
        String fileName = StringUtils.hasText(audioFile.getOriginalFilename())
                ? Path.of(audioFile.getOriginalFilename()).getFileName().toString()
                : "voice.webm";
        String extension = extension(fileName);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(415, "仅支持 wav、mp3、m4a、webm 格式的录音");
        }
        String normalizedLanguage = StringUtils.hasText(language) ? language.trim() : "zh-CN";
        if (normalizedLanguage.length() > 20) {
            throw new BusinessException(400, "language 格式不正确");
        }
        try {
            return new AudioInput(
                    audioFile.getBytes(),
                    fileName,
                    extension,
                    mimeType(extension),
                    normalizedLanguage);
        } catch (IOException exception) {
            throw new BusinessException(500, "读取录音文件失败");
        }
    }

    private VoiceRecords.DigitalAssetRow saveAudio(AudioInput audio, Integer durationMs) {
        String checksum = sha256(audio.bytes());
        VoiceRecords.DigitalAssetRow existing = voiceMapper.findAudioAssetByChecksum(checksum);
        if (existing != null) {
            return existing;
        }

        Path storageDirectory = Path.of(properties.audioStorageDir()).toAbsolutePath().normalize();
        Path storedFile = storageDirectory.resolve(UUID.randomUUID() + "." + audio.extension()).normalize();
        if (!storedFile.startsWith(storageDirectory)) {
            throw new BusinessException(500, "录音存储路径不安全");
        }
        try {
            Files.createDirectories(storageDirectory);
            Files.write(storedFile, audio.bytes());
        } catch (IOException exception) {
            throw new BusinessException(500, "保存录音文件失败");
        }

        VoiceRecords.DigitalAssetRow asset = new VoiceRecords.DigitalAssetRow();
        asset.setStorageUri(storedFile.toUri().toString());
        asset.setMimeType(audio.mimeType());
        asset.setChecksumSha256(checksum);
        asset.setDurationMs(durationMs);
        try {
            voiceMapper.insertAudioAsset(asset);
            return asset;
        } catch (DuplicateKeyException exception) {
            deleteQuietly(storedFile);
            VoiceRecords.DigitalAssetRow duplicate = voiceMapper.findAudioAssetByChecksum(checksum);
            if (duplicate != null) {
                return duplicate;
            }
            throw exception;
        } catch (RuntimeException exception) {
            deleteQuietly(storedFile);
            throw exception;
        }
    }

    private Session resolveSession(String requestedToken) {
        String token = normalizeOrGenerateToken(requestedToken);
        Long existing = voiceMapper.findSessionIdByToken(token);
        if (existing != null) {
            return new Session(existing, token);
        }
        VoiceRecords.SessionRow session = new VoiceRecords.SessionRow();
        session.setSessionToken(token);
        try {
            voiceMapper.insertSession(session);
            return new Session(session.getId(), token);
        } catch (DuplicateKeyException exception) {
            Long concurrent = voiceMapper.findSessionIdByToken(token);
            if (concurrent != null) {
                return new Session(concurrent, token);
            }
            throw exception;
        }
    }

    private String normalizeOrGenerateToken(String requestedToken) {
        if (!StringUtils.hasText(requestedToken)) {
            return UUID.randomUUID().toString();
        }
        try {
            return UUID.fromString(requestedToken.trim()).toString();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "sessionToken 必须是合法的 UUID");
        }
    }

    private ResolvedIntent resolveIntent(String content) {
        VoiceAiClient.IntentAnalysis modelIntent = voiceAiClient.analyzeIntent(content).orElse(null);
        if (modelIntent != null && isSupportedIntent(modelIntent.intentType())) {
            String songTitle = normalizeOptional(modelIntent.songTitle());
            String artistName = normalizeOptional(modelIntent.artistName());
            if ("SONG_SEARCH".equals(modelIntent.intentType()) && songTitle == null && artistName == null) {
                songTitle = extractSongTitle(content);
            }
            return new ResolvedIntent(
                    modelIntent.intentType(),
                    defaultConfidence(modelIntent.confidence(), "UNKNOWN".equals(modelIntent.intentType())
                            ? "0.4000" : "0.9000"),
                    songTitle,
                    artistName);
        }
        return ruleBasedIntent(content);
    }

    private ResolvedIntent ruleBasedIntent(String content) {
        boolean songCommand = containsAny(content, SONG_COMMAND_WORDS) || BOOK_TITLE.matcher(content).find();
        boolean recommendation = containsAny(content, RECOMMEND_WORDS)
                && (content.contains("想听") || content.contains("来点") || content.contains("推荐"));
        if (recommendation && BOOK_TITLE.matcher(content).results().findAny().isEmpty()) {
            return new ResolvedIntent("SONG_RECOMMEND", new BigDecimal("0.8600"), null, null);
        }
        if (songCommand) {
            return new ResolvedIntent(
                    "SONG_SEARCH",
                    new BigDecimal("0.8800"),
                    extractSongTitle(content),
                    extractArtist(content));
        }
        if (containsAny(content, QA_WORDS) || content.endsWith("吗") || content.endsWith("呢")) {
            return new ResolvedIntent("KNOWLEDGE_QA", new BigDecimal("0.8500"), null, null);
        }
        return new ResolvedIntent("UNKNOWN", new BigDecimal("0.4000"), null, null);
    }

    private String extractSongTitle(String content) {
        Matcher quoted = BOOK_TITLE.matcher(content);
        if (quoted.find()) {
            return normalizeOptional(quoted.group(1));
        }
        String result = content.trim();
        for (String command : SONG_COMMAND_WORDS) {
            result = result.replace(command, "");
        }
        for (String prefix : List.of(
                "麻烦给我", "请给我", "能不能", "可以", "请", "我要", "我想", "给我", "帮我", "一下")) {
            result = result.replace(prefix, "");
        }
        result = ARTIST_PATTERN.matcher(result).replaceAll("");
        result = result.replaceAll("(这首|那首)?(歌曲|曲子|乐曲|古筝曲)$", "");
        result = result.replaceAll("[吧呀啊啦。！？!?，,]+$", "");
        return normalizeOptional(result);
    }

    private String extractArtist(String content) {
        Matcher matcher = ARTIST_PATTERN.matcher(content);
        return matcher.find() ? normalizeOptional(matcher.group(1)) : null;
    }

    private MatchedSong matchSong(String songTitle, String artistName) {
        if (!StringUtils.hasText(songTitle) && !StringUtils.hasText(artistName)) {
            return new MatchedSong(false, ZERO_SCORE, null);
        }
        List<ScoredSong> scored = voiceMapper.findReadySongs().stream()
                .map(song -> scoreSong(song, songTitle, artistName))
                .sorted(Comparator.comparing(ScoredSong::score).reversed()
                        .thenComparing(item -> item.titleExact() ? 0 : 1)
                        .thenComparing(item -> item.artistExact() ? 0 : 1)
                        .thenComparing(item -> item.song().getDisplayOrder(),
                                Comparator.nullsLast(Integer::compareTo)))
                .toList();
        if (scored.isEmpty()) {
            return new MatchedSong(false, ZERO_SCORE, null);
        }
        ScoredSong top = scored.get(0);
        boolean matched = top.score().compareTo(MATCH_THRESHOLD) >= 0;
        return new MatchedSong(matched, top.score(), matched ? top.song() : null);
    }

    private ScoredSong scoreSong(
            VoiceRecords.SongRow song,
            String requestedTitle,
            String requestedArtist) {
        String title = normalizeForMatch(song.getTitle());
        String queryTitle = normalizeForMatch(requestedTitle);
        BigDecimal titleScore = BigDecimal.ZERO;
        boolean titleExact = false;
        if (StringUtils.hasText(queryTitle)) {
            titleScore = similarity(queryTitle, title);
            titleExact = queryTitle.equals(title);
            if (StringUtils.hasText(song.getAliases())) {
                for (String alias : song.getAliases().split(",")) {
                    titleScore = titleScore.max(similarity(queryTitle, normalizeForMatch(alias)));
                    titleExact = titleExact || queryTitle.equals(normalizeForMatch(alias));
                }
            }
        }

        String queryArtist = normalizeForMatch(requestedArtist);
        String artist = normalizeForMatch(song.getArtistName());
        BigDecimal artistScore = StringUtils.hasText(queryArtist)
                ? similarity(queryArtist, artist)
                : BigDecimal.ZERO;
        boolean artistExact = StringUtils.hasText(queryArtist) && queryArtist.equals(artist);

        BigDecimal score;
        if (StringUtils.hasText(queryTitle) && StringUtils.hasText(queryArtist)) {
            score = titleScore.multiply(new BigDecimal("0.7500"))
                    .add(artistScore.multiply(new BigDecimal("0.2500")));
        } else {
            score = StringUtils.hasText(queryTitle) ? titleScore : artistScore;
        }
        return new ScoredSong(song, scaleScore(score), titleExact, artistExact);
    }

    private BigDecimal similarity(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return BigDecimal.ZERO;
        }
        if (left.equals(right)) {
            return BigDecimal.ONE;
        }
        if (left.contains(right) || right.contains(left)) {
            double ratio = (double) Math.min(left.length(), right.length())
                    / Math.max(left.length(), right.length());
            return BigDecimal.valueOf(Math.max(0.75, ratio));
        }
        int distance = levenshtein(left, right);
        double result = 1.0 - ((double) distance / Math.max(left.length(), right.length()));
        return BigDecimal.valueOf(Math.max(0.0, result));
    }

    private int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int index = 0; index <= right.length(); index++) {
            previous[index] = index;
        }
        for (int row = 1; row <= left.length(); row++) {
            current[0] = row;
            for (int column = 1; column <= right.length(); column++) {
                int substitution = previous[column - 1]
                        + (left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1);
                current[column] = Math.min(
                        Math.min(current[column - 1] + 1, previous[column] + 1),
                        substitution);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    private VoiceInteractionDtos.SongMatch toMatch(
            MatchedSong result,
            String message,
            boolean showStartButton) {
        VoiceRecords.SongRow song = result.song();
        return new VoiceInteractionDtos.SongMatch(
                result.matched(),
                MATCH_THRESHOLD,
                result.score(),
                song == null ? null : song.getSongId(),
                song == null ? null : song.getTitle(),
                song == null ? null : song.getArtistName(),
                song == null ? null : song.getCoverUrl(),
                message,
                showStartButton);
    }

    private List<VoiceRecords.KnowledgeRow> retrieveKnowledge(String question, int topK) {
        return voiceMapper.findKnowledgeCandidates().stream()
                .peek(item -> item.setRelevanceScore(scoreKnowledge(question, item)))
                .filter(item -> item.getRelevanceScore().compareTo(new BigDecimal("0.1200")) >= 0)
                .sorted(Comparator.comparing(VoiceRecords.KnowledgeRow::getRelevanceScore).reversed()
                        .thenComparing(VoiceRecords.KnowledgeRow::getSourceId))
                .limit(topK)
                .toList();
    }

    private BigDecimal scoreKnowledge(String question, VoiceRecords.KnowledgeRow item) {
        String normalizedQuestion = normalizeForMatch(question);
        String title = normalizeForMatch(item.getTitle());
        String content = normalizeForMatch(item.getContent());
        Set<String> queryBigrams = ngrams(normalizedQuestion, 2);
        Set<String> candidateBigrams = ngrams(title + content, 2);
        long overlap = queryBigrams.stream().filter(candidateBigrams::contains).count();
        double score = queryBigrams.isEmpty() ? 0.0 : 0.65 * overlap / queryBigrams.size();
        if (normalizedQuestion.contains(title) || title.contains(normalizedQuestion)) {
            score += 0.3;
        }
        if ("INSTRUMENT_HISTORY".equals(item.getSourceType())
                && containsAny(question, List.of("历史", "起源", "最早", "时期", "年代", "秦筝"))) {
            score += 0.25;
        }
        if ("INSTRUMENT_STRUCTURE".equals(item.getSourceType())
                && containsAny(question, List.of("结构", "部件", "琴弦", "琴码", "面板", "作用", "位置"))) {
            score += 0.25;
        }
        if ("SONG".equals(item.getSourceType())
                && containsAny(question, List.of("曲目", "乐曲", "歌曲", "作者", "背景"))) {
            score += 0.2;
        }
        return scaleScore(BigDecimal.valueOf(Math.min(1.0, score)));
    }

    private Set<String> ngrams(String text, int size) {
        if (!StringUtils.hasText(text)) {
            return Set.of();
        }
        if (text.length() <= size) {
            return Set.of(text);
        }
        Set<String> values = new LinkedHashSet<>();
        for (int index = 0; index <= text.length() - size; index++) {
            values.add(text.substring(index, index + size));
        }
        return values;
    }

    private String fallbackAnswer(List<VoiceRecords.KnowledgeRow> references) {
        if (references.isEmpty()) {
            return "当前知识库中没有足够资料可靠回答这个问题，请换一个更具体的古筝问题。";
        }
        VoiceRecords.KnowledgeRow first = references.get(0);
        return "根据“" + first.getTitle() + "”的资料，" + first.getContent();
    }

    private void saveAnswerSources(Long answerId, List<VoiceRecords.KnowledgeRow> references) {
        List<VoiceRecords.AnswerSourceRow> sources = new ArrayList<>();
        int rank = 1;
        for (VoiceRecords.KnowledgeRow reference : references) {
            if (reference.getKnowledgeItemId() == null) {
                continue;
            }
            VoiceRecords.AnswerSourceRow source = new VoiceRecords.AnswerSourceRow();
            source.setAnswerId(answerId);
            source.setKnowledgeItemId(reference.getKnowledgeItemId());
            source.setRankNo(rank++);
            source.setRelevanceScore(reference.getRelevanceScore());
            sources.add(source);
        }
        if (!sources.isEmpty()) {
            voiceMapper.insertAnswerSources(sources);
        }
    }

    private String normalizeChannel(String inputChannel) {
        String channel = StringUtils.hasText(inputChannel)
                ? inputChannel.trim().toUpperCase(Locale.ROOT)
                : "TEXT";
        if (!Set.of("TEXT", "VOICE").contains(channel)) {
            throw new BusinessException(400, "inputChannel 仅支持 TEXT 或 VOICE");
        }
        return channel;
    }

    private String toDatabaseIntent(String apiIntent) {
        return switch (apiIntent) {
            case "KNOWLEDGE_QA" -> "QA";
            case "SONG_SEARCH" -> "SONG_SEARCH";
            case "SONG_RECOMMEND" -> "RECOMMENDATION";
            default -> "OTHER";
        };
    }

    private boolean isSupportedIntent(String intentType) {
        return Set.of("SONG_SEARCH", "SONG_RECOMMEND", "KNOWLEDGE_QA", "UNKNOWN")
                .contains(intentType);
    }

    private boolean containsAny(String value, List<String> words) {
        return words.stream().anyMatch(value::contains);
    }

    private String normalizeForMatch(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return NON_TEXT.matcher(value.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal defaultConfidence(BigDecimal value, String defaultValue) {
        return value == null ? new BigDecimal(defaultValue) : value;
    }

    private BigDecimal scaleConfidence(BigDecimal value) {
        return scaleScore(value == null ? BigDecimal.ZERO : value);
    }

    private BigDecimal scaleScore(BigDecimal value) {
        BigDecimal clamped = value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        return clamped.setScale(4, RoundingMode.HALF_UP);
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String mimeType(String extension) {
        return switch (extension) {
            case "wav" -> "audio/wav";
            case "mp3" -> "audio/mpeg";
            case "m4a" -> "audio/mp4";
            default -> "audio/webm";
        };
    }

    private String sha256(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder value = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                value.append(String.format("%02x", item));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 Java 环境不支持 SHA-256", exception);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 数据库已保存的资源优先；临时残留文件由运维任务清理。
        }
    }

    private record AudioInput(
            byte[] bytes,
            String fileName,
            String extension,
            String mimeType,
            String language) {
    }

    private record Session(Long id, String token) {
    }

    private record ResolvedIntent(
            String intentType,
            BigDecimal confidence,
            String songTitle,
            String artistName) {
    }

    private record ScoredSong(
            VoiceRecords.SongRow song,
            BigDecimal score,
            boolean titleExact,
            boolean artistExact) {
    }

    private record MatchedSong(boolean matched, BigDecimal score, VoiceRecords.SongRow song) {
    }
}
