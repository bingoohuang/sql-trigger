package com.github.bingoohuang.sqltrigger;

import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Map;

public class ProxyFactoryUpdate extends ProxyFactoryPrepare {
    private final String sql;
    private final SqlTriggerBeanMap sqlTriggerBeanMap;
    private final SQLUpdateStatement stmt;
    private final List<TriggerBeanItem> items;

    public ProxyFactoryUpdate(String sql, SqlTriggerBeanMap sqlTriggerBeanMap, SQLUpdateStatement stmt) {
        this.sql = sql;
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
        val variantVisitor = new VariantVisitor();
        val setCols = createUpdateColumnInfo(variantVisitor);
        val cols = SqlParseUtil.createWhereColumnInfo(stmt.getWhere());
        Object[] triggerBeans = sqlTriggerBeanMap.getTriggerBeans();
        return new ProxyImpl(sql, ps, Lists.newArrayList(cols), setCols, items, triggerBeans, variantVisitor.getVariantIndex()).create();
    }

    private Map<Integer, TriggerColumnInfo> createUpdateColumnInfo(VariantVisitor variantVisitor) {
        Map<Integer, TriggerColumnInfo> setCols = Maps.newHashMap();
        int index = 0;

        for (val item : stmt.getItems()) {
            ++index;

            val column = item.getColumn();
            if (column instanceof SQLIdentifierExpr) {
                val simpleName = ((SQLIdentifierExpr) column).getSimpleName();
                createUpdateTriggerColumn(setCols, index, simpleName, item, variantVisitor);
            } else if (column instanceof SQLPropertyExpr) {
                val simpleName = ((SQLPropertyExpr) column).getSimpleName();
                createUpdateTriggerColumn(setCols, index, simpleName, item, variantVisitor);
            }
        }
        return setCols;
    }

    private void createUpdateTriggerColumn(Map<Integer, TriggerColumnInfo> setCols, int index, String itemName,
                                           SQLUpdateSetItem item, VariantVisitor variantVisitor) {
        val col = new TriggerColumnInfo(itemName.toUpperCase());
        SqlParseUtil.fulfilColumnInfo(col, item.getValue());

        item.accept(variantVisitor);
        col.setVarIndex(variantVisitor.getVarIndex());
        setCols.put(index, col);
    }
}
