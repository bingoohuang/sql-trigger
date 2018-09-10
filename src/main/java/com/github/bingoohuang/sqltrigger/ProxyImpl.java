package com.github.bingoohuang.sqltrigger;

import lombok.RequiredArgsConstructor;
import lombok.val;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class ProxyImpl {
    private final String sql;
    private final Object preparedStatement;
    private final List<Map<Integer, TriggerColumnInfo>> colsList;
    private final Map<Integer, TriggerColumnInfo> setCols;
    private final List<TriggerBeanItem> items;
    private final Object[] filterBeans;
    private final AtomicInteger varIndex;

    public Object create() {
        fulfilVarIndex();
        return Proxy.newProxyInstance(ProxyImpl.class.getClassLoader(),
                new Class[]{PreparedStatement.class},
                new PsInvocationHandler(sql, preparedStatement, items, colsList, setCols, filterBeans));
    }

    private void fulfilVarIndex() {
        for (val cols : colsList) {
            incrementVariantRef(cols);
        }
    }

    private void incrementVariantRef(Map<Integer, TriggerColumnInfo> cols) {
        for (val e : cols.entrySet()) {
            val columnInfo = e.getValue();
            if (columnInfo.getValueType() == ValueType.VariantRef) {
                columnInfo.setVarIndex(varIndex.incrementAndGet());
            }
        }
    }

}
