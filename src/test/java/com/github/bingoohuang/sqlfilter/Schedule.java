package com.github.bingoohuang.sqlfilter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Schedule {
    private String id;
    private boolean idMapped;
    private String name;
    private boolean nameMapped;

    @SqlFilterColumn(value = "schedule_state", mappedField = "stateUsed")
    private String state;

    private boolean stateUsed;

    private boolean noneMapped;

    private int subscribes;
}
