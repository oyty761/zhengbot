package com.guzheng.mapper;

import com.guzheng.entity.PartResource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PartResourceMapper {

    List<PartResource> selectByPartIdAndRole(@Param("partId") Long partId, @Param("resourceRole") String resourceRole);
}
