package com.guzheng.explore.mapper;

import com.guzheng.explore.entity.InstrumentPart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InstrumentPartMapper {

    InstrumentPart selectById(@Param("id") Long id);

    List<InstrumentPart> selectEnabledPartsByKind(@Param("partKind") String partKind);

    InstrumentPart selectStringPartByStringNo(@Param("stringNo") Integer stringNo);
}
