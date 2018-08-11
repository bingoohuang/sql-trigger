package com.github.bingoohuang.sqlfilter;

import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class ProxyDelete implements ProxyPrepare {
    private final SQLDeleteStatement stmt;

    @Override
    public Object create(FilterParser filterParser, Object ps, Object[] filterBeans) {
        val tableName = stmt.getTableName().getSimpleName();
        val items = filterParser.findByFilterType(tableName, FilterType.DELETE);
        if (items.isEmpty()) return ps;

        val cols = SqlParseUtil.createWhereColumnInfo(stmt.getWhere());
        return new ProxyImpl(ps, Lists.newArrayList(cols), null, items, filterBeans).create();
    }
}
