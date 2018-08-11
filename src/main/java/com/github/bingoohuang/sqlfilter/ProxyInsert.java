package com.github.bingoohuang.sqlfilter;

import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.List;
import java.util.Map;

import static com.github.bingoohuang.sqlfilter.SqlParseUtil.fulfilColumnInfo;

@RequiredArgsConstructor
public class ProxyInsert implements ProxyPrepare {
    private final SQLInsertStatement stmt;

    @Override public Object create(FilterParser filterParser, Object ps, Object[] filterBeans) {
        val tableName = stmt.getTableName().getSimpleName();
        val items = filterParser.findByFilterType(tableName, FilterType.INSERT);
        if (items.isEmpty()) return ps;

        val cols = createSqlInsertColumns();
        val colsList = fulfilSqlInsertColumns(cols);

        return new ProxyImpl(ps, Lists.newArrayList(colsList), null, items, filterBeans).create();

    }

    private List<Map<Integer, ColumnInfo>> fulfilSqlInsertColumns(Map<Integer, ColumnInfo> prototype) {
        List<Map<Integer, ColumnInfo>> list = Lists.newArrayList();

        for (val values : stmt.getValuesList()) {
            Map<Integer, ColumnInfo> cols = SqlParseUtil.clone(prototype);
            list.add(cols);

            int index = 0;
            for (val value : values.getValues()) {
                ++index;
                val col = cols.get(index);
                if (col == null) continue;

                fulfilColumnInfo(value, col);
            }
        }

        return list;
    }

    private Map<Integer, ColumnInfo> createSqlInsertColumns() {
        Map<Integer, ColumnInfo> cols = Maps.newHashMap();

        int index = 0;
        for (val col : stmt.getColumns()) {
            ++index;

            if (col instanceof SQLIdentifierExpr) {
                val expr = (SQLIdentifierExpr) col;
                val simpleName = expr.getSimpleName();
                cols.put(index, new ColumnInfo(simpleName.toUpperCase()));
            }
        }

        return cols;
    }
}
