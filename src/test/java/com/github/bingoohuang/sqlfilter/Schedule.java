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

    private String scheduleState;

    private boolean noneMapped;
}
