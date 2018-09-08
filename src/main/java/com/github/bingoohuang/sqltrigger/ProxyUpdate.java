package com.github.bingoohuang.sqltrigger;

import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;

import java.util.Map;

@RequiredArgsConstructor
public class ProxyUpdate implements ProxyPrepare {
    private final SQLUpdateStatement stmt;

    @Override
    public Object create(SqlTriggerParser sqlTriggerParser, Object ps, Object[] filterBeans) {
        val tableName = stmt.getTableName().getSimpleName();
        val items = sqlTriggerParser.findByFilterType(tableName, TriggerType.UPDATE);
        if (CollectionUtils.isEmpty(items)) return ps;

        val setCols = createUpdateColumnInfo();
        val cols = SqlParseUtil.createWhereColumnInfo(stmt.getWhere());
        return new ProxyImpl(ps, Lists.newArrayList(cols), setCols, items, filterBeans).create();
    }

    private Map<Integer, ColumnInfo> createUpdateColumnInfo() {
        Map<Integer, ColumnInfo> setCols = Maps.newHashMap();
        int index = 0;
        for (val item : stmt.getItems()) {
            ++index;

            val column = item.getColumn();
            if (column instanceof SQLIdentifierExpr) {
                val expr = (SQLIdentifierExpr) column;
                val col = new ColumnInfo(expr.getSimpleName().toUpperCase());
                SqlParseUtil.fulfilColumnInfo(item.getValue(), col);
                setCols.put(index, col);
            }
        }
        return setCols;
    }
}
