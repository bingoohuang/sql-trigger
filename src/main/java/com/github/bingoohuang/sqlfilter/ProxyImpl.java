package com.github.bingoohuang.sqlfilter;

import lombok.RequiredArgsConstructor;
import lombok.val;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class ProxyImpl {
    private final Object preparedStatement;
    private final List<Map<Integer, ColumnInfo>> colsList;
    private final Map<Integer, ColumnInfo> setCols;
    private final List<FilterItem> items;
    private final Object[] filterBeans;

    public Object create() {
        fulfilVarIndex();
        return Proxy.newProxyInstance(ProxyImpl.class.getClassLoader(),
                new Class[]{PreparedStatement.class},
                new PreparedStatementHandler(preparedStatement, items, colsList, setCols, filterBeans));
    }

    private void fulfilVarIndex() {
        int varIndex = 0;
        if (setCols != null) {
            varIndex = incrementVariantRef(setCols, varIndex);
        }

        for (val setCols1 : colsList) {
            varIndex = incrementVariantRef(setCols1, varIndex);
        }
    }

    private int incrementVariantRef(Map<Integer, ColumnInfo> setCols, int varIndex) {
        for (val e : setCols.entrySet()) {
            if (e.getValue().getValueType() == ValueType.VariantRef) {
                e.getValue().setVarIndex(++varIndex);
            }
        }

        return varIndex;
    }

}
