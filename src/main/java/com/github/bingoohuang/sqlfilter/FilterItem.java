package com.github.bingoohuang.sqlfilter;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;

@Data @RequiredArgsConstructor
public class FilterItem {
    private final FilterType type;
    private final Method method;
}

