package com.github.bingoohuang.sqltrigger;

import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.bingoohuang.sqltrigger.SqlParseUtil.fulfilColumnInfo;

public class ProxyFactoryInsert extends ProxyFactoryPrepare {
    private final String sql;
    private final SqlTriggerBeanMap sqlTriggerBeanMap;
    private final SQLInsertStatement stmt;
    private final List<TriggerBeanItem> items;

    public ProxyFactoryInsert(String sql, SqlTriggerBeanMap sqlTriggerBeanMap, SQLInsertStatement stmt) {
        this.sql = sql;
        this.sqlTriggerBeanMap = sqlTriggerBeanMap;
        this.stmt = stmt;

        val tableName = stmt.getTableName().getSimpleName();
        this.items = sqlTriggerBeanMap.findByTriggerType(tableName, TriggerType.INSERT);
    }

    @Override public boolean requiredProxy() {
        return CollectionUtils.isNotEmpty(items);
    }

    @Override public Object createPsProxyFactory(Object ps) {
        val cols = createSqlInsertColumns();
        val colsList = fulfilSqlInsertColumns(cols);

        Object[] triggerBeans = sqlTriggerBeanMap.getTriggerBeans();
        return new ProxyImpl(sql, ps, Lists.newArrayList(colsList), null, items, triggerBeans, new AtomicInteger()).create();

    }

    private List<Map<Integer, TriggerColumnInfo>> fulfilSqlInsertColumns(Map<Integer, TriggerColumnInfo> prototype) {
        List<Map<Integer, TriggerColumnInfo>> list = Lists.newArrayList();

        val variantVisitor = new VariantVisitor();
        for (val values : stmt.getValuesList()) {
            val cols = SqlParseUtil.clone(prototype);
            list.add(cols);

            int index = 0;
            for (val value : values.getValues()) {
                value.accept(variantVisitor);
                val col = cols.get(++index);
                if (col != null) {
                    fulfilColumnInfo(col, value);
                    col.setVarIndex(variantVisitor.getVarIndex());
                }
            }
        }

        return list;
    }

    private Map<Integer, TriggerColumnInfo> createSqlInsertColumns() {
        Map<Integer, TriggerColumnInfo> cols = Maps.newHashMap();

        int index = 0;
        for (val col : stmt.getColumns()) {
            ++index;

            if (col instanceof SQLIdentifierExpr) {
                val simpleName = ((SQLIdentifierExpr) col).getSimpleName();
                cols.put(index, new TriggerColumnInfo(simpleName.toUpperCase()));
            }
        }

        return cols;
    }
}
