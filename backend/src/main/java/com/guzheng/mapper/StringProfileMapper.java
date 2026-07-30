package com.guzheng.mapper;

import com.guzheng.entity.StringProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StringProfileMapper {

    StringProfile selectByStringNo(@Param("stringNo") Integer stringNo);
}
