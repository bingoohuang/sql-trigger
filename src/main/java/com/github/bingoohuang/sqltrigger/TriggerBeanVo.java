package com.github.bingoohuang.sqltrigger;

import com.google.common.collect.Lists;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class TriggerBeanVo {
    private final List<TriggerBeanItem> items = Lists.newArrayList();

    public void add(TriggerType type, Method method) {
        items.add(new TriggerBeanItem(type, method));
    }

    public List<TriggerBeanItem> filter(TriggerType triggerType) {
        return items.stream().filter(x -> x.getType() == triggerType).collect(Collectors.toList());
    }
}