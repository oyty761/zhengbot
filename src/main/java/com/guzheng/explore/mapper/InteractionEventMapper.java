package com.guzheng.explore.mapper;

import com.guzheng.explore.entity.InteractionEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InteractionEventMapper {

    int insert(InteractionEvent event);
}
