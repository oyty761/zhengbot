package com.guzheng.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Song implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long workId;
    private String artistName;
    private String originPeriod;
    private String backgroundText;
    private String styleText;
    private String featuredExcerpt;
}
