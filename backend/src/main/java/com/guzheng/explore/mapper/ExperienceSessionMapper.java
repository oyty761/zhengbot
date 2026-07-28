package com.guzheng.explore.mapper;

import com.guzheng.explore.entity.ExperienceSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ExperienceSessionMapper {

    ExperienceSession selectBySessionToken(@Param("sessionToken") String sessionToken);
}
