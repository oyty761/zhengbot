package com.guzheng.explore.mapper;

import com.guzheng.explore.entity.PlayableWork;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PlayableWorkMapper {

    PlayableWork selectById(@Param("id") Long id);
}
