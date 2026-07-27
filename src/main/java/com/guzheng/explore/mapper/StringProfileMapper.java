package com.guzheng.explore.mapper;

import com.guzheng.explore.entity.StringProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StringProfileMapper {

    StringProfile selectByStringNo(@Param("stringNo") Integer stringNo);
}
