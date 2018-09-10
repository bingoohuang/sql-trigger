package com.github.bingoohuang.sqltrigger;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.*;
import com.github.bingoohuang.utils.lang.Str;
import com.google.common.collect.Maps;
import lombok.val;

import java.util.Map;

public class SqlParseUtil {

    public static Map<Integer, TriggerColumnInfo> clone(Map<Integer, TriggerColumnInfo> prototype) {
        Map<Integer, TriggerColumnInfo> map = Maps.newHashMap();

        for (val e : prototype.entrySet()) {
            map.put(e.getKey(), new TriggerColumnInfo(e.getValue().getName()));
        }

        return map;
    }

    public static void fulfilColumnInfo(TriggerColumnInfo col, SQLExpr value) {
        if (value instanceof SQLVariantRefExpr) {
            col.setValueType(ValueType.VariantRef);
        } else if (value instanceof SQLTextLiteralExpr) {
            col.setValueType(ValueType.Literal);
            col.setValue(((SQLTextLiteralExpr) value).getText());
        } else if (value instanceof SQLIntegerExpr) {
            col.setValueType(ValueType.Literal);
            col.setValue(((SQLIntegerExpr) value).getNumber());
        }
    }

    public static Map<Integer, TriggerColumnInfo> createWhereColumnInfo(SQLExpr where) {
        Map<Integer, TriggerColumnInfo> cols = Maps.newHashMap();
        processWhereItems(where, cols);

        return cols;
    }

    private static void processWhereItems(SQLExpr where, Map<Integer, TriggerColumnInfo> cols) {
        if (where instanceof SQLBinaryOpExpr) {
            val e = (SQLBinaryOpExpr) where;
            val l = e.getLeft();
            val r = e.getRight();
            val o = e.getOperator().getName();

            if (Str.anyOf(o, "=", "!=", "<>", ">", ">=", "<", "<=")) {
                if (l instanceof SQLIdentifierExpr) {
                    val simpleName = ((SQLIdentifierExpr) l).getSimpleName();
                    createWhereTriggerColumn(cols, r, simpleName);
                } else if (l instanceof SQLPropertyExpr) {
                    val simpleName = ((SQLPropertyExpr) l).getSimpleName();
                    createWhereTriggerColumn(cols, r, simpleName);
                }
            } else if (Str.anyOf(o, "AND", "OR")) {
                processWhereItems(l, cols);
                processWhereItems(r, cols);
            }
        }
    }

    private static void createWhereTriggerColumn(Map<Integer, TriggerColumnInfo> cols, SQLExpr r, String simpleName) {
        int colIndex = cols.size() + 1;

        val columnInfo = new TriggerColumnInfo(simpleName.toUpperCase());
        cols.put(colIndex, columnInfo);
        fulfilColumnInfo(columnInfo, r);
    }

}
