package com.guzheng.explore.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentPart implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String partCode;
    private String partKind;
    private String name;
    private String summary;
    private String functionText;
    private String positionText;
    private String performanceRelation;
    private Integer displayOrder;
    private Boolean enabled;
}
