package com.guzheng.mapper;

import com.guzheng.entity.HistorySong;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HistorySongMapper {

    List<HistorySong> selectByHistoryEntryId(@Param("historyEntryId") Long historyEntryId);
}
