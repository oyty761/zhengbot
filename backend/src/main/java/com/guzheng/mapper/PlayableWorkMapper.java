package com.guzheng.mapper;

import com.guzheng.entity.PlayableWork;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PlayableWorkMapper {

    PlayableWork selectById(@Param("id") Long id);
}
