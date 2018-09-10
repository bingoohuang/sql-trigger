package com.github.bingoohuang.sqltrigger;

import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.Map;

@Value @RequiredArgsConstructor
public class SqlTriggerContext {
    private final String sql;
    private final Map<Integer, Object> params;
}
