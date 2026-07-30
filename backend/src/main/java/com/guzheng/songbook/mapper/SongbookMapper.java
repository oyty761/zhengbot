package com.guzheng.songbook.mapper;

import com.guzheng.songbook.model.SongbookRecords;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SongbookMapper {

    List<SongbookRecords.SongRow> findPage(
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size);

    long countSongs(@Param("keyword") String keyword);

    SongbookRecords.SongRow findSongDetail(@Param("songId") Long songId);

    List<SongbookRecords.SongRow> searchSongs(
            @Param("query") String query,
            @Param("limit") int limit);

    List<SongbookRecords.SongRow> recommendSongs(
            @Param("descriptorIds") List<Long> descriptorIds,
            @Param("descriptorCount") int descriptorCount,
            @Param("limit") int limit);

    List<SongbookRecords.SongRow> fallbackSongs(@Param("limit") int limit);

    List<SongbookRecords.DescriptorRow> findRecommendationDescriptors();

    List<SongbookRecords.DescriptorRow> findFeedbackDescriptors();

    int countFeedbackDescriptors(@Param("descriptorIds") List<Long> descriptorIds);

    int countDigitalAsset(@Param("assetId") Long assetId);

    Long findSessionIdByToken(@Param("sessionToken") String sessionToken);

    int insertSession(SongbookRecords.SessionRow session);

    int insertUtterance(SongbookRecords.UtteranceRow utterance);

    int insertDiscoveryRequest(SongbookRecords.DiscoveryRequestRow request);

    int insertCandidates(@Param("candidates") List<SongbookRecords.DiscoveryCandidateRow> candidates);

    int completeDiscoveryRequest(
            @Param("requestId") Long requestId,
            @Param("status") String status);

    int markCandidateSelectedByRequest(
            @Param("requestId") Long requestId,
            @Param("songId") Long songId);

    int insertPerformance(SongbookRecords.PerformanceRow performance);

    SongbookRecords.PerformanceRow findPerformance(@Param("performanceId") Long performanceId);

    Long findFeedbackIdByPerformance(@Param("performanceId") Long performanceId);

    int insertFeedback(SongbookRecords.FeedbackRow feedback);

    int insertFeedbackDescriptors(
            @Param("feedbackId") Long feedbackId,
            @Param("descriptorIds") List<Long> descriptorIds);
}

