package com.github.bingoohuang.sqltrigger;

import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;

@RequiredArgsConstructor
public class ProxyDelete implements ProxyPrepare {
    private final SQLDeleteStatement stmt;

    @Override
    public Object create(SqlTriggerParser sqlTriggerParser, Object ps, Object[] filterBeans) {
        val tableName = stmt.getTableName().getSimpleName();
        val items = sqlTriggerParser.findByFilterType(tableName, TriggerType.DELETE);
        if (CollectionUtils.isEmpty(items)) return ps;

        val cols = SqlParseUtil.createWhereColumnInfo(stmt.getWhere());
        return new ProxyImpl(ps, Lists.newArrayList(cols), null, items, filterBeans).create();
    }
}
