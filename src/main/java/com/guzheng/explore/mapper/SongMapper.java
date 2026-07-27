package com.guzheng.explore.mapper;

import com.guzheng.explore.entity.Song;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SongMapper {

    Song selectByWorkId(@Param("workId") Long workId);
}
