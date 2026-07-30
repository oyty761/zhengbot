package com.guzheng.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StructureOverviewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private DigitalAssetDTO modelAsset;
    private List<PartOverviewDTO> keyParts;
}
