package com.guzheng.explore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExploreHomeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<ExploreModuleDTO> modules;
}
