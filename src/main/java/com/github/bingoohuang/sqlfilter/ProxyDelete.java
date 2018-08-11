package com.github.bingoohuang.sqlfilter;

import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.lang.reflect.Method;
import java.sql.Connection;

import static com.github.bingoohuang.sqlfilter.ReflectUtil.invokeMethod;

@RequiredArgsConstructor
public class ProxyDelete implements ProxyPrepare {
    private final SQLDeleteStatement stmt;
    private final Method method;
    private final Object[] args;

    @Override
    public Object create(FilterParser filterParser, Connection conn, Object filter) {
        val items = filterParser.findByFilterType(stmt.getTableName().getSimpleName(), FilterType.DELETE);
        if (items.isEmpty()) return invokeMethod(method, args);

        val cols = SqlParseUtil.createWhereColumnInfo(stmt.getWhere());
        val ps = invokeMethod(method, conn, args);

        return new ProxyImpl(ps, Lists.newArrayList(cols), null, items, filter).create();
    }
}
