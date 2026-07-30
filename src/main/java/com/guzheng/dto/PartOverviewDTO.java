package com.guzheng.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartOverviewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long partId;
    private String name;
    private String desc;
    private String imageUrl;
}
