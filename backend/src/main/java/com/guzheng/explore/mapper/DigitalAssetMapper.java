package com.guzheng.explore.mapper;

import com.guzheng.explore.entity.DigitalAsset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DigitalAssetMapper {

    DigitalAsset selectById(@Param("id") Long id);

    DigitalAsset selectFirstByKind(@Param("assetKind") String assetKind);
}
