package com.guzheng.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StringHighlightDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer stringNo;
    private Boolean highlight;
    private String pitch;
    private String region;
    private String audioUrl;
    private String action;
}
