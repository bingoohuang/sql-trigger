package com.github.bingoohuang.sqlfilter;

import lombok.Value;

import java.lang.reflect.Method;

@Value
public class FilterItem {
    private final FilterType type;
    private final Method method;
}

