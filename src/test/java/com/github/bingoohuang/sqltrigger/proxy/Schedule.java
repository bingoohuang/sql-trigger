package com.github.bingoohuang.sqltrigger.proxy;

import com.github.bingoohuang.sqltrigger.SqlTriggerColumn;
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

    @SqlTriggerColumn(value = "schedule_state", mappedField = "stateUsed")
    private String state;

    private boolean stateUsed;

    private boolean noneMapped;

    private int subscribes;
}
