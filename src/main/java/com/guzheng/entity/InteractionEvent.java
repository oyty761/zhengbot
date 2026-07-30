package com.guzheng.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InteractionEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long sessionId;
    private String eventType;
    private String sourceModule;
    private String targetModule;
    private Long sourceId;
    private Long targetId;
    private String payload;
    private LocalDateTime createdAt;
}
