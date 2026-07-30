package com.guzheng.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryStageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long stageId;
    private String title;
    private String desc;
    private String time;
}
