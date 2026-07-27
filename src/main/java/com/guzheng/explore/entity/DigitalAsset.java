package com.guzheng.explore.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DigitalAsset implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String assetKind;
    private String storageUri;
    private String mimeType;
    private String checksumSha256;
    private Integer durationMs;
    private LocalDateTime createdAt;
}
