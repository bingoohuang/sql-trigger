package com.github.bingoohuang.sqlfilter;

import com.google.common.collect.Lists;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class FilterVo {
    private final List<FilterItem> items = Lists.newArrayList();

    public void add(FilterType type, Method method) {
        items.add(new FilterItem(type, method));
    }

    public List<FilterItem> filter(FilterType filterType) {
        return items.stream().filter(x -> x.getType() == filterType).collect(Collectors.toList());
    }
}