package com.github.bingoohuang.sqltrigger;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data @RequiredArgsConstructor
public class TriggerColumnInfo {
    private final String name;
    private int varIndex;
    private ValueType valueType;
    private Object value;
}
