package com.guzheng.mapper;

import com.guzheng.entity.DigitalAsset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DigitalAssetMapper {

    DigitalAsset selectById(@Param("id") Long id);

    DigitalAsset selectFirstByKind(@Param("assetKind") String assetKind);
}
