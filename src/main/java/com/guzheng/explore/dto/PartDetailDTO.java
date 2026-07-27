package com.guzheng.explore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartDetailDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long partId;
    private String name;
    private String function;
    private String position;
    private String playRelation;
    private List<String> images;
}
