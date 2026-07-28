package com.guzheng.explore.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartResourceId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long partId;
    private Long assetId;
    private String resourceRole;
}
