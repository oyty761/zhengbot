package com.guzheng.explore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongLinkDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String targetModule;
    private Long targetSongId;
    private String msg;
}
