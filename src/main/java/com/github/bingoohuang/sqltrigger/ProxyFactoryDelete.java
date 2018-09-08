package com.github.bingoohuang.sqltrigger;

import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.google.common.collect.Lists;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

public class ProxyFactoryDelete extends ProxyFactoryPrepare {
    private final SqlTriggerBeanMap sqlTriggerBeanMap;
    private final SQLDeleteStatement stmt;
    private final List<TriggerBeanItem> items;


    public ProxyFactoryDelete(SqlTriggerBeanMap sqlTriggerBeanMap, SQLDeleteStatement stmt) {
        this.sqlTriggerBeanMap = sqlTriggerBeanMap;
        this.stmt = stmt;
        val tableName = stmt.getTableName().getSimpleName();
        this.items = sqlTriggerBeanMap.findByTriggerType(tableName, TriggerType.DELETE);
    }

    @Override public boolean requiredProxy() {
        return CollectionUtils.isNotEmpty(items);
    }

    @Override
    public Object createPsProxyFactory(Object ps) {
        val cols = SqlParseUtil.createWhereColumnInfo(stmt.getWhere());
        Object[] triggerBeans = sqlTriggerBeanMap.getTriggerBeans();
        return new ProxyImpl(ps, Lists.newArrayList(cols), null, items, triggerBeans).create();
    }
}
