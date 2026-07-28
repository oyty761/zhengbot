package com.guzheng.explore.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    private String entryKind;
    private String title;
    private String timeLabel;
    private Short startYear;
    private Short endYear;
    private String content;
    private Long coverAssetId;
    private Integer displayOrder;
    private Boolean enabled;
}
