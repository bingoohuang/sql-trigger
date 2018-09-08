package com.github.bingoohuang.sqltrigger;

import lombok.Value;

import java.lang.reflect.Method;

@Value
public class TriggerBeanItem {
    private final TriggerType type;
    private final Method method;
}

