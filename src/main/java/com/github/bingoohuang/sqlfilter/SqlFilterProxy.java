package com.github.bingoohuang.sqlfilter;


import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLTextLiteralExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.util.JdbcConstants;
import com.github.bingoohuang.utils.lang.Str;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import lombok.val;
import org.n3r.eql.joor.Reflect;

import java.lang.reflect.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SqlFilterProxy {
    public static Connection create(Connection conn, Object filter) {
        Map<String, FilterVo> map = Maps.newHashMap();

        Method[] methods = filter.getClass().getMethods();
        for (val method : methods) {
            val sqlFilter = method.getAnnotation(SqlFilter.class);
            if (sqlFilter == null) continue;

            val upperCaseTable = sqlFilter.table().toUpperCase();
            FilterVo filterVo = map.get(upperCaseTable);
            if (filterVo == null) {
                filterVo = new FilterVo();
                map.put(upperCaseTable, filterVo);
            }

            filterVo.add(sqlFilter.type(), method);
        }

        return (Connection) proxyConnection(conn, map, filter);
    }

    private static Object proxyConnection(Connection conn, Map<String, FilterVo> map, Object filter) {
        return Proxy.newProxyInstance(SqlFilterProxy.class.getClassLoader(),
                new Class[]{Connection.class}, (proxy, method, args) -> {
                    if (method.getName().equals("prepareStatement")) {
                        return proxyPreparedStatement(method, args, map, conn, filter);
                    }

                    return method.invoke(conn, args);
                });
    }

    @SneakyThrows
    private static Object proxyPreparedStatement(Method method, Object[] args, Map<String, FilterVo> map, Connection conn, Object filter) {
        val sql = (String) args[0];
        val sqlStatements = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);

        val sqlStatement = sqlStatements.get(0);
        if (sqlStatement instanceof SQLInsertStatement) {
            return proxyInsertPreparedStatement(method, args, map, conn, (SQLInsertStatement) sqlStatement, filter);
        }

        return method.invoke(conn, args);
    }

    @SneakyThrows
    private static Object proxyInsertPreparedStatement(Method method, Object[] args, Map<String, FilterVo> map, Connection conn, SQLInsertStatement insertStmt, Object filter) {
        val tableName = insertStmt.getTableName().getSimpleName();

        val filterVo = map.get(tableName.toUpperCase());
        if (filterVo == null) {
            return method.invoke(conn, args);
        }

        val items = filterVo.findByFilterType(FilterType.INSERT);
        if (items.isEmpty()) {
            return method.invoke(conn, args);
        }


        val cols = createSqlInsertColumns(insertStmt);
        fulfilSqlInsertColumns(insertStmt, cols);

        val ps = method.invoke(conn, args);
        return proxyFilteredInsertPreparedStatement(ps, cols, items, filter);

    }

    private static Object proxyFilteredInsertPreparedStatement(Object ps, Map<Integer, ColumnInfo> cols, List<FilterItem> items, Object filter) {
        return Proxy.newProxyInstance(SqlFilterProxy.class.getClassLoader(),
                new Class[]{PreparedStatement.class}, new InvocationHandler() {
                    Map<Integer, Object> parameters = null;

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (Str.anyOf(method.getName(), "setString", "setObject")) {
                            int parameterIndex = (Integer) args[0];
                            if (parameters == null)
                                parameters = Maps.newHashMap();
                            parameters.put(parameterIndex, args[1]);
                        } else if (method.getName().equals("executeUpdate")) {
                            invokeFilter(cols, items, filter, parameters);
                            parameters = null;
                        }

                        return method.invoke(ps, args);
                    }
                });
    }


    private static void invokeFilter(Map<Integer, ColumnInfo> cols, List<FilterItem> items, Object filter, Map<Integer, Object> parameters) {
        for (val item : items) {
            invokeFilter(cols, item, filter, parameters);
        }
    }

    @SneakyThrows
    private static void invokeFilter(Map<Integer, ColumnInfo> cols, FilterItem item, Object filter, Map<Integer, Object> parameters) {
        Method method = item.getMethod();

        List<Object> args = Lists.newArrayList();

        for (val parameter : method.getParameters()) {
            if (parameter.getType() == SqlFilterContext.class) {
                args.add(new SqlFilterContext());
            } else {
                args.add(createBean(parameter, cols, parameters));
            }
        }

        method.invoke(filter, args.toArray(new Object[args.size()]));
    }

    @SneakyThrows
    private static Object createBean(Parameter parameter, Map<Integer, ColumnInfo> cols, Map<Integer, Object> parameters) {
        Class<?> parameterType = parameter.getType();
        val param = Reflect.on(parameterType).create().get();

        for (val field : parameterType.getDeclaredFields()) {
            val allowedNames = createAllowedNames(field.getName());

            val columnInfo = findColumn(cols, allowedNames);
            if (columnInfo == null) continue;

            setAccessible(field);

            if (columnInfo.getValueType() == ValueType.VariantRef) {
                field.set(param, parameters.get(columnInfo.getIndex()));
            } else if (columnInfo.getValueType() == ValueType.TextLiteral) {
                field.set(param, columnInfo.getValue());
            }

            setMapped(parameterType, param, field.getName());
        }

        return param;
    }

    private static void setAccessible(Field field) {
        if (!field.isAccessible()) field.setAccessible(true);
    }

    private static void setMapped(Class<?> parameterType, Object param, String fieldName) throws IllegalAccessException {
        val mappedField = findField(parameterType, fieldName + "Mapped");
        if (mappedField == null) return;

        setAccessible(mappedField);
        mappedField.set(param, Boolean.TRUE);
    }

    private static ColumnInfo findColumn(Map<Integer, ColumnInfo> cols, Set<String> names) {
        for (val e : cols.entrySet()) {
            if (names.contains(e.getValue().getName())) return e.getValue();
        }

        return null;
    }

    private static Set<String> createAllowedNames(String fieldName) {
        return Sets.newHashSet(fieldName.toUpperCase(), toUpperUnderScore(fieldName));
    }

    private static String toUpperUnderScore(String fieldName) {
        val sb = new StringBuilder();

        char[] chars = fieldName.toCharArray();
        sb.append(chars[0]);
        for (int i = 1; i < chars.length; ++i) {
            if (Character.isUpperCase(chars[i])) {
                sb.append("_");
            }
            sb.append(chars[i]);
        }

        return sb.toString().toUpperCase();
    }

    private static Field findField(Class<?> type, String fieldName) {
        for (val field : type.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) return field;
        }

        return null;
    }

    private static void fulfilSqlInsertColumns(SQLInsertStatement insertStmt, Map<Integer, ColumnInfo> cols) {
        int index = 0;
        for (val value : insertStmt.getValues().getValues()) {
            val col = cols.get(++index);
            if (col == null) continue;

            if (value instanceof SQLVariantRefExpr) {
                col.setValueType(ValueType.VariantRef);
            } else if (value instanceof SQLTextLiteralExpr) {
                col.setValueType(ValueType.TextLiteral);
                col.setValue(((SQLTextLiteralExpr) value).getText());
            }
        }
    }

    private static Map<Integer, ColumnInfo> createSqlInsertColumns(SQLInsertStatement insertStmt) {
        Map<Integer, ColumnInfo> cols = Maps.newHashMap();

        int index = 0;
        for (val col : insertStmt.getColumns()) {
            ++index;

            if (col instanceof SQLIdentifierExpr) {
                val identifierExpr = (SQLIdentifierExpr) col;
                String simpleName = identifierExpr.getSimpleName();

                cols.put(index, new ColumnInfo(index, simpleName.toUpperCase()));
            }
        }

        return cols;
    }
}