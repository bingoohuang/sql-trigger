package com.github.bingoohuang.sqltrigger;

import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Map;

import static com.github.bingoohuang.sqltrigger.SqlParseUtil.fulfilColumnInfo;

@RequiredArgsConstructor
public class ProxyInsert implements ProxyPrepare {
    private final SQLInsertStatement stmt;

    @Override public Object create(SqlTriggerParser sqlTriggerParser, Object ps, Object[] filterBeans) {
        val tableName = stmt.getTableName().getSimpleName();
        val items = sqlTriggerParser.findByFilterType(tableName, TriggerType.INSERT);
        if (CollectionUtils.isEmpty(items)) return ps;

        val cols = createSqlInsertColumns();
        val colsList = fulfilSqlInsertColumns(cols);

        return new ProxyImpl(ps, Lists.newArrayList(colsList), null, items, filterBeans).create();

    }

    private List<Map<Integer, ColumnInfo>> fulfilSqlInsertColumns(Map<Integer, ColumnInfo> prototype) {
        List<Map<Integer, ColumnInfo>> list = Lists.newArrayList();

        for (val values : stmt.getValuesList()) {
            val cols = SqlParseUtil.clone(prototype);
            list.add(cols);

            int index = 0;
            for (val value : values.getValues()) {
                val col = cols.get(++index);
                if (col != null) fulfilColumnInfo(value, col);
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
                val simpleName = ((SQLIdentifierExpr) col).getSimpleName();
                cols.put(index, new ColumnInfo(simpleName.toUpperCase()));
            }
        }

        return cols;
    }
}
