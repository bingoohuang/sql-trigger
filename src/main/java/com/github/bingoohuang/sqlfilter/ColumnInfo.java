package com.github.bingoohuang.sqlfilter;

import lombok.Data;

@Data
public class ColumnInfo {
    private int varIndex;
    private String name;
    private ValueType valueType;
    private String value;

    public ColumnInfo(String name) {
        this.name = name;
    }

    public ColumnInfo(String name, ValueType valueType, String value) {
        this.name = name;
        this.valueType = valueType;
        this.value = value;
    }
}