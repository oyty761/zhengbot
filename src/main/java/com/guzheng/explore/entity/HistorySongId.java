package com.guzheng.explore.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorySongId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long historyEntryId;
    private Long songId;
}
