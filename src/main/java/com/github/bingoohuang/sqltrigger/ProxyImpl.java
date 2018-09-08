package com.github.bingoohuang.sqltrigger;

import lombok.RequiredArgsConstructor;
import lombok.val;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class ProxyImpl {
    private final Object preparedStatement;
    private final List<Map<Integer, TriggerColumnInfo>> colsList;
    private final Map<Integer, TriggerColumnInfo> setCols;
    private final List<TriggerBeanItem> items;
    private final Object[] filterBeans;

    public Object create() {
        fulfilVarIndex();
        return Proxy.newProxyInstance(ProxyImpl.class.getClassLoader(),
                new Class[]{PreparedStatement.class},
                new PsInvocationHandler(preparedStatement, items, colsList, setCols, filterBeans));
    }

    private void fulfilVarIndex() {
        int varIndex = 0;
        if (setCols != null) {
            varIndex = incrementVariantRef(setCols, varIndex);
        }

        for (val cols : colsList) {
            varIndex = incrementVariantRef(cols, varIndex);
        }
    }

    private int incrementVariantRef(Map<Integer, TriggerColumnInfo> cols, int varIndex) {
        for (val e : cols.entrySet()) {
            val columnInfo = e.getValue();
            if (columnInfo.getValueType() == ValueType.VariantRef) {
                columnInfo.setVarIndex(++varIndex);
            }
        }

        return varIndex;
    }

}
