package com.guzheng.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StageSongItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long songId;
    private String name;
    private String bg;
}
