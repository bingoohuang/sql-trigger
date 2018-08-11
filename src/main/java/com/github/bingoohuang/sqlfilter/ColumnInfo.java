package com.github.bingoohuang.sqlfilter;

import lombok.Data;

@Data
public class ColumnInfo {
    private int index;
    private String name;
    private ValueType valueType;
    private String value;

    public ColumnInfo(int index, String name) {
        this.index = index;
        this.name = name;
    }
}