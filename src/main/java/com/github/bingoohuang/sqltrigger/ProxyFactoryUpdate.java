package com.github.bingoohuang.sqltrigger;

import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Map;

public class ProxyFactoryUpdate extends ProxyFactoryPrepare {
    private final SqlTriggerBeanMap sqlTriggerBeanMap;
    private final SQLUpdateStatement stmt;
    private final List<TriggerBeanItem> items;

    public ProxyFactoryUpdate(SqlTriggerBeanMap sqlTriggerBeanMap, SQLUpdateStatement stmt) {
        this.sqlTriggerBeanMap = sqlTriggerBeanMap;
        this.stmt = stmt;
        val tableName = stmt.getTableName().getSimpleName();
        this.items = sqlTriggerBeanMap.findByTriggerType(tableName, TriggerType.UPDATE);
    }

    @Override public boolean requiredProxy() {
        return CollectionUtils.isNotEmpty(items);
    }

    @Override
    public Object createPsProxyFactory(Object ps) {
        val setCols = createUpdateColumnInfo();
        val cols = SqlParseUtil.createWhereColumnInfo(stmt.getWhere());
        Object[] triggerBeans = sqlTriggerBeanMap.getTriggerBeans();
        return new ProxyImpl(ps, Lists.newArrayList(cols), setCols, items, triggerBeans).create();
    }

    private Map<Integer, TriggerColumnInfo> createUpdateColumnInfo() {
        Map<Integer, TriggerColumnInfo> setCols = Maps.newHashMap();
        int index = 0;
        for (val item : stmt.getItems()) {
            ++index;

            val column = item.getColumn();
            if (column instanceof SQLIdentifierExpr) {
                val expr = (SQLIdentifierExpr) column;
                val col = new TriggerColumnInfo(expr.getSimpleName().toUpperCase());
                SqlParseUtil.fulfilColumnInfo(item.getValue(), col);
                setCols.put(index, col);
            }
        }
        return setCols;
    }
}
