package com.github.bingoohuang.sqlfilter;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.*;
import com.github.bingoohuang.utils.lang.Str;
import com.google.common.collect.Maps;
import lombok.val;

import java.util.Map;

public class SqlParseUtil {

    public static Map<Integer, ColumnInfo> clone(Map<Integer, ColumnInfo> prototype) {
        Map<Integer, ColumnInfo> map = Maps.newHashMap();

        for (val e : prototype.entrySet()) {
            map.put(e.getKey(), new ColumnInfo(e.getValue().getName()));
        }

        return map;
    }

    public static void fulfilColumnInfo(SQLExpr value, ColumnInfo col) {
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

    public static Map<Integer, ColumnInfo> createWhereColumnInfo(SQLExpr where) {
        Map<Integer, ColumnInfo> cols = Maps.newHashMap();
        processWhereItems(where, cols);

        return cols;
    }

    private static void processWhereItems(SQLExpr where, Map<Integer, ColumnInfo> cols) {
        if (where instanceof SQLBinaryOpExpr) {
            val e = (SQLBinaryOpExpr) where;
            val l = e.getLeft();
            val r = e.getRight();
            val o = e.getOperator().getName();

            if (Str.anyOf(o, "=", "!=", "<>", ">", ">=", "<", "<=")) {
                if (l instanceof SQLIdentifierExpr) {
                    val simpleName = ((SQLIdentifierExpr) l).getSimpleName().toUpperCase();
                    int colIndex = cols.size() + 1;

                    val columnInfo = new ColumnInfo(simpleName);
                    cols.put(colIndex, columnInfo);
                    fulfilColumnInfo(r, columnInfo);
                }
            } else if (Str.anyOf(o, "AND", "OR")) {
                processWhereItems(l, cols);
                processWhereItems(r, cols);
            }
        }
    }

}
