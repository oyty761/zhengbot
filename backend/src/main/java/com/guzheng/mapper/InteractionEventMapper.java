package com.guzheng.mapper;

import com.guzheng.entity.InteractionEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InteractionEventMapper {

    int insert(InteractionEvent event);
}
