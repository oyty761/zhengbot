package com.guzheng.voice.mapper;

import com.guzheng.voice.model.VoiceRecords;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VoiceInteractionMapper {

    Long findSessionIdByToken(@Param("sessionToken") String sessionToken);

    int insertSession(VoiceRecords.SessionRow session);

    VoiceRecords.DigitalAssetRow findAudioAssetByChecksum(@Param("checksum") String checksum);

    int insertAudioAsset(VoiceRecords.DigitalAssetRow asset);

    int insertUtterance(VoiceRecords.UtteranceRow utterance);

    VoiceRecords.UtteranceRow findUtterance(@Param("utteranceId") Long utteranceId);

    List<VoiceRecords.SongRow> findReadySongs();

    List<VoiceRecords.KnowledgeRow> findKnowledgeCandidates();

    int insertQaAnswer(VoiceRecords.QaAnswerRow answer);

    int insertAnswerSources(@Param("sources") List<VoiceRecords.AnswerSourceRow> sources);
}
