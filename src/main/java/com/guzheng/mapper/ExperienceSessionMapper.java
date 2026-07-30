package com.guzheng.mapper;

import com.guzheng.entity.ExperienceSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ExperienceSessionMapper {

    ExperienceSession selectBySessionToken(@Param("sessionToken") String sessionToken);
}
