package com.github.bingoohuang.sqlfilter;

import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Map;

import static com.github.bingoohuang.sqlfilter.ReflectUtil.invokeMethod;

@RequiredArgsConstructor
public class ProxyUpdate implements ProxyPrepare {
    private final SQLUpdateStatement stmt;
    private final Method method;
    private final Object[] args;

    @Override
    public Object create(FilterParser filterParser, Connection conn, Object filter) {
        val items = filterParser.findByFilterType(stmt.getTableName().getSimpleName(), FilterType.UPDATE);
        if (items.isEmpty()) return invokeMethod(method, args);

        val setCols = createUpdateColumnInfo();
        val cols = SqlParseUtil.createWhereColumnInfo(stmt.getWhere());
        val ps = invokeMethod(method, conn, args);

        return new ProxyImpl(ps, Lists.newArrayList(cols), setCols, items, filter).create();
    }

    private Map<Integer, ColumnInfo> createUpdateColumnInfo() {
        Map<Integer, ColumnInfo> setCols = Maps.newHashMap();
        int index = 0;
        for (val item : stmt.getItems()) {
            ++index;

            val itemColumn = item.getColumn();
            if (itemColumn instanceof SQLIdentifierExpr) {
                val col = new ColumnInfo(((SQLIdentifierExpr) itemColumn).getSimpleName().toUpperCase());
                SqlParseUtil.fulfilColumnInfo(item.getValue(), col);
                setCols.put(index, col);
            }
        }
        return setCols;
    }
}
