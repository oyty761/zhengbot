package com.guzheng.explore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExploreModuleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String type;
    private String desc;
}
