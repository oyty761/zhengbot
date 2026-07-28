package com.guzheng.explore.mapper;

import com.guzheng.explore.entity.HistorySong;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HistorySongMapper {

    List<HistorySong> selectByHistoryEntryId(@Param("historyEntryId") Long historyEntryId);
}
