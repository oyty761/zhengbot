package com.guzheng.service.impl;

import com.guzheng.common.BusinessException;
import com.guzheng.dto.*;
import com.guzheng.entity.*;
import com.guzheng.mapper.*;
import com.guzheng.service.InstrumentExploreService;
import com.guzheng.util.PitchConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InstrumentExploreServiceImpl implements InstrumentExploreService {

    private final DigitalAssetMapper digitalAssetMapper;
    private final InstrumentPartMapper instrumentPartMapper;
    private final PartResourceMapper partResourceMapper;
    private final StringProfileMapper stringProfileMapper;
    private final HistoryEntryMapper historyEntryMapper;
    private final SongMapper songMapper;
    private final HistorySongMapper historySongMapper;
    private final ExperienceSessionMapper experienceSessionMapper;
    private final InteractionEventMapper interactionEventMapper;
    private final PlayableWorkMapper playableWorkMapper;

    private static final String ASSET_KIND_MODEL = "MODEL";
    private static final String PART_KIND_COMPONENT = "COMPONENT";
    private static final String RESOURCE_ROLE_DETAIL_IMAGE = "DETAIL_IMAGE";
    private static final String RESOURCE_ROLE_DEMO_AUDIO = "DEMO_AUDIO";
    private static final String HISTORY_ENTRY_KIND_PERIOD = "PERIOD";
    private static final String EVENT_TYPE_SONG_LINK = "SONG_LINK";
    private static final String SOURCE_MODULE_EXPLORE = "EXPLORE";
    private static final String TARGET_MODULE_SONGBOOK = "SONGBOOK";

    @Override
    public ExploreHomeDTO getHomeModules() {
        List<ExploreModuleDTO> modules = new ArrayList<>();
        modules.add(new ExploreModuleDTO("乐器立体结构讲解", "STRUCTURE", "3D模型+部件讲解"));
        modules.add(new ExploreModuleDTO("乐器历史讲解", "HISTORY", "时间轴+文化故事"));
        return new ExploreHomeDTO(modules);
    }

    @Override
    public StructureOverviewDTO getStructureOverview() {
        DigitalAsset modelAsset = digitalAssetMapper.selectFirstByKind(ASSET_KIND_MODEL);
        DigitalAssetDTO modelAssetDTO = null;
        if (modelAsset != null) {
            modelAssetDTO = new DigitalAssetDTO(modelAsset.getId(), modelAsset.getStorageUri(), "3D_MODEL");
        }

        List<InstrumentPart> parts = instrumentPartMapper.selectEnabledPartsByKind(PART_KIND_COMPONENT);
        List<PartOverviewDTO> keyParts = new ArrayList<>();
        if (!CollectionUtils.isEmpty(parts)) {
            for (InstrumentPart part : parts) {
                String imageUrl = findFirstAssetUrl(part.getId(), RESOURCE_ROLE_DETAIL_IMAGE);
                keyParts.add(new PartOverviewDTO(part.getId(), part.getName(), part.getSummary(), imageUrl));
            }
        }

        return new StructureOverviewDTO(modelAssetDTO, keyParts);
    }

    @Override
    public PartDetailDTO getPartDetail(Long partId) {
        if (partId == null || partId <= 0) {
            throw new BusinessException(400, "部件编号不合法");
        }
        InstrumentPart part = instrumentPartMapper.selectById(partId);
        if (part == null || Boolean.FALSE.equals(part.getEnabled())) {
            throw new BusinessException(404, "部件不存在");
        }

        List<String> images = findAssetUrls(partId, RESOURCE_ROLE_DETAIL_IMAGE);
        return new PartDetailDTO(
                part.getId(),
                part.getName(),
                part.getFunctionText(),
                part.getPositionText(),
                part.getPerformanceRelation(),
                images
        );
    }

    @Override
    public StringHighlightDTO highlightString(Integer stringNo, String action) {
        if (stringNo == null || stringNo < 1 || stringNo > 64) {
            throw new BusinessException(400, "弦号必须在1~64之间");
        }

        StringProfile profile = stringProfileMapper.selectByStringNo(stringNo);
        if (profile == null) {
            throw new BusinessException(404, "琴弦不存在");
        }

        InstrumentPart part = instrumentPartMapper.selectStringPartByStringNo(stringNo);
        if (part == null || Boolean.FALSE.equals(part.getEnabled())) {
            throw new BusinessException(404, "琴弦部件不存在");
        }

        String audioUrl = findFirstAssetUrl(part.getId(), RESOURCE_ROLE_DEMO_AUDIO);
        String pitch = PitchConverter.midiToPitch(profile.getMidiNote());

        StringHighlightDTO dto = new StringHighlightDTO();
        dto.setStringNo(stringNo);
        dto.setPitch(pitch);
        dto.setRegion(profile.getRegisterName());
        dto.setAudioUrl(audioUrl);

        if ("play".equalsIgnoreCase(action)) {
            dto.setAction("play");
            dto.setHighlight(null);
        } else {
            dto.setHighlight(true);
            dto.setAction("highlight");
        }
        return dto;
    }

    @Override
    public List<HistoryStageDTO> getHistoryTimeline() {
        List<HistoryEntry> periods = historyEntryMapper.selectEnabledPeriods();
        List<HistoryStageDTO> stages = new ArrayList<>();
        if (CollectionUtils.isEmpty(periods)) {
            return stages;
        }
        for (HistoryEntry entry : periods) {
            stages.add(new HistoryStageDTO(entry.getId(), entry.getTitle(), entry.getContent(), entry.getTimeLabel()));
        }
        return stages;
    }

    @Override
    public StageSongsDTO getStageSongs(Long stageId) {
        if (stageId == null || stageId <= 0) {
            throw new BusinessException(400, "历史阶段编号不合法");
        }
        HistoryEntry stage = historyEntryMapper.selectById(stageId);
        if (stage == null || Boolean.FALSE.equals(stage.getEnabled()) || !HISTORY_ENTRY_KIND_PERIOD.equals(stage.getEntryKind())) {
            throw new BusinessException(404, "历史阶段不存在");
        }

        List<HistorySong> historySongs = historySongMapper.selectByHistoryEntryId(stageId);
        List<StageSongItemDTO> songs = new ArrayList<>();
        if (!CollectionUtils.isEmpty(historySongs)) {
            for (HistorySong historySong : historySongs) {
                Song song = songMapper.selectByWorkId(historySong.getSongId());
                if (song != null) {
                    songs.add(new StageSongItemDTO(song.getWorkId(), findSongDisplayName(song), song.getBackgroundText()));
                }
            }
        }
        return new StageSongsDTO(stageId, songs);
    }

    @Override
    public SongLinkDTO linkSongToSongbook(Long songId, String sessionId) {
        if (songId == null || songId <= 0) {
            throw new BusinessException(400, "曲目编号不合法");
        }
        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException(400, "会话ID不能为空");
        }

        Song song = songMapper.selectByWorkId(songId);
        if (song == null) {
            throw new BusinessException(404, "曲目不存在");
        }

        ExperienceSession session = experienceSessionMapper.selectBySessionToken(sessionId);
        if (session == null) {
            throw new BusinessException(404, "会话不存在");
        }

        InteractionEvent event = new InteractionEvent();
        event.setSessionId(session.getId());
        event.setEventType(EVENT_TYPE_SONG_LINK);
        event.setSourceModule(SOURCE_MODULE_EXPLORE);
        event.setTargetModule(TARGET_MODULE_SONGBOOK);
        event.setSourceId(songId);
        event.setTargetId(songId);
        event.setPayload("{\"action\":\"history_to_songbook\",\"songId\":" + songId + "}");
        event.setCreatedAt(LocalDateTime.now());
        try {
            interactionEventMapper.insert(event);
        } catch (Exception e) {
            // 记录日志但不阻塞跳转逻辑
        }

        return new SongLinkDTO(TARGET_MODULE_SONGBOOK, songId, "已跳转至点歌模块");
    }

    private String findFirstAssetUrl(Long partId, String resourceRole) {
        List<PartResource> resources = partResourceMapper.selectByPartIdAndRole(partId, resourceRole);
        if (CollectionUtils.isEmpty(resources)) {
            return null;
        }
        DigitalAsset asset = digitalAssetMapper.selectById(resources.get(0).getAssetId());
        return asset == null ? null : asset.getStorageUri();
    }

    private List<String> findAssetUrls(Long partId, String resourceRole) {
        List<PartResource> resources = partResourceMapper.selectByPartIdAndRole(partId, resourceRole);
        if (CollectionUtils.isEmpty(resources)) {
            return Collections.emptyList();
        }
        List<String> urls = new ArrayList<>();
        for (PartResource resource : resources) {
            DigitalAsset asset = digitalAssetMapper.selectById(resource.getAssetId());
            if (asset != null) {
                urls.add(asset.getStorageUri());
            }
        }
        return urls;
    }

    private String findSongDisplayName(Song song) {
        if (song == null || song.getWorkId() == null) {
            return "";
        }
        PlayableWork work = playableWorkMapper.selectById(song.getWorkId());
        return work != null && work.getTitle() != null ? work.getTitle() : "";
    }
}
