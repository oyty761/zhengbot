package com.guzheng.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartMarker implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long partId;
    private String markerCode;
    private String modelNode;
    private BigDecimal positionX;
    private BigDecimal positionY;
    private BigDecimal positionZ;
}
