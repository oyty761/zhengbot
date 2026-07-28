package com.guzheng.explore.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayableWork implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String workKind;
    private String title;
    private String playableStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
