package com.guzheng.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorySong implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long historyEntryId;
    private Long songId;
    private Integer displayOrder;
}
