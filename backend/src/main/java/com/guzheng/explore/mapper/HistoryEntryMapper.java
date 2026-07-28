package com.guzheng.explore.mapper;

import com.guzheng.explore.entity.HistoryEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HistoryEntryMapper {

    HistoryEntry selectById(@Param("id") Long id);

    List<HistoryEntry> selectEnabledPeriods();

    List<HistoryEntry> selectChildrenByParentId(@Param("parentId") Long parentId);
}
