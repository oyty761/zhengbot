package com.guzheng.mapper;

import com.guzheng.entity.Song;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SongMapper {

    Song selectByWorkId(@Param("workId") Long workId);
}
