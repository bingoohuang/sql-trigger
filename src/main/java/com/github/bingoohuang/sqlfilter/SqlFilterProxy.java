package com.github.bingoohuang.sqlfilter;


import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLTextLiteralExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
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
            return proxyPreparedStatementInsert(method, args, map, conn, (SQLInsertStatement) sqlStatement, filter);
        } else if (sqlStatement instanceof SQLDeleteStatement) {
            return proxyPreparedStatementDelete(method, args, map, conn, (SQLDeleteStatement) sqlStatement, filter);
        } else if (sqlStatement instanceof SQLUpdateStatement) {
            return proxyPreparedStatementUpdate(method, args, map, conn, (SQLUpdateStatement) sqlStatement, filter);
        }

        return method.invoke(conn, args);
    }

    @SneakyThrows
    private static Object proxyPreparedStatementUpdate(Method method, Object[] args, Map<String, FilterVo> map, Connection conn, SQLUpdateStatement stmt, Object filter) {
        val tableName = stmt.getTableName().getSimpleName();
        val filterVo = map.get(tableName.toUpperCase());
        if (filterVo == null) return method.invoke(conn, args);

        val items = filterVo.findByFilterType(FilterType.UPDATE);
        if (items.isEmpty()) return method.invoke(conn, args);

        val setCols = createUpdateColumnInfo(stmt);
        val cols = createWhereColumnInfo(stmt.getWhere());

        val ps = method.invoke(conn, args);


        return proxyFilteredPreparedStatement(ps, cols, setCols, items, filter);
    }

    private static Map<Integer, ColumnInfo> createWhereColumnInfo(SQLExpr where) {
        Map<Integer, ColumnInfo> cols = Maps.newHashMap();
        if (where != null) processWhereItems(where, cols);

        return cols;
    }

    private static Map<Integer, ColumnInfo> createUpdateColumnInfo(SQLUpdateStatement stmt) {
        Map<Integer, ColumnInfo> setCols = Maps.newHashMap();
        int index = 0;
        for (val item : stmt.getItems()) {
            ++index;

            val itemColumn = item.getColumn();
            if (itemColumn instanceof SQLIdentifierExpr) {
                val col = new ColumnInfo(((SQLIdentifierExpr) itemColumn).getSimpleName().toUpperCase());

                val itemValue = item.getValue();
                if (itemValue instanceof SQLTextLiteralExpr) {
                    col.setValueType(ValueType.TextLiteral);
                    col.setValue(((SQLTextLiteralExpr) itemValue).getText());
                } else if (itemValue instanceof SQLVariantRefExpr) {
                    col.setValueType(ValueType.VariantRef);
                }

                setCols.put(index, col);
            }
        }
        return setCols;
    }

    @SneakyThrows
    private static Object proxyPreparedStatementDelete(Method method, Object[] args, Map<String, FilterVo> map, Connection conn, SQLDeleteStatement stmt, Object filter) {
        val tableName = stmt.getTableName().getSimpleName();
        val filterVo = map.get(tableName.toUpperCase());
        if (filterVo == null) return method.invoke(conn, args);

        val items = filterVo.findByFilterType(FilterType.DELETE);
        if (items.isEmpty()) return method.invoke(conn, args);

        val cols = createWhereColumnInfo(stmt.getWhere());

        val ps = method.invoke(conn, args);

        return proxyFilteredPreparedStatement(ps, cols, null, items, filter);
    }

    private static void processWhereItems(SQLExpr where, Map<Integer, ColumnInfo> cols) {
        if (where instanceof SQLBinaryOpExpr) {
            val expr = (SQLBinaryOpExpr) where;
            val left = expr.getLeft();
            val right = expr.getRight();
            val operator = expr.getOperator().getName();

            if (Str.anyOf(operator, "=", "!=", "<>", ">", ">=", "<", "<=")) {
                if (left instanceof SQLIdentifierExpr) {
                    val simpleName = ((SQLIdentifierExpr) left).getSimpleName().toUpperCase();
                    int colIndex = cols.size() + 1;
                    if (right instanceof SQLTextLiteralExpr) {
                        cols.put(colIndex, new ColumnInfo(simpleName, ValueType.TextLiteral, ((SQLTextLiteralExpr) right).getText()));
                    } else if (right instanceof SQLVariantRefExpr) {
                        cols.put(colIndex, new ColumnInfo(simpleName, ValueType.VariantRef, null));
                    }
                }
            } else if (Str.anyOf(operator, "AND", "OR")) {
                processWhereItems(left, cols);
                processWhereItems(right, cols);
            }
        }
    }

    @SneakyThrows
    private static Object proxyPreparedStatementInsert(Method method, Object[] args, Map<String, FilterVo> map, Connection conn, SQLInsertStatement stmt, Object filter) {
        val tableName = stmt.getTableName().getSimpleName();

        val filterVo = map.get(tableName.toUpperCase());
        if (filterVo == null) return method.invoke(conn, args);

        val items = filterVo.findByFilterType(FilterType.INSERT);
        if (items.isEmpty()) return method.invoke(conn, args);


        val cols = createSqlInsertColumns(stmt);
        fulfilSqlInsertColumns(stmt, cols);

        val ps = method.invoke(conn, args);
        return proxyFilteredPreparedStatement(ps, cols, null, items, filter);

    }

    private static Object proxyFilteredPreparedStatement(Object ps, Map<Integer, ColumnInfo> cols, Map<Integer, ColumnInfo> setCols, List<FilterItem> items, Object filter) {
        fulfilVarIndex(cols, setCols);

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
                            invokeFilter(cols, setCols, items, filter, parameters);
                            parameters = null;
                        }

                        return method.invoke(ps, args);
                    }
                });
    }

    private static void fulfilVarIndex(Map<Integer, ColumnInfo> cols, Map<Integer, ColumnInfo> setCols) {
        int varIndex = 0;
        if (setCols != null) {
            for (val e : setCols.entrySet()) {
                if (e.getValue().getValueType() == ValueType.VariantRef) {
                    e.getValue().setVarIndex(++varIndex);
                }
            }
        }

        for (val e : cols.entrySet()) {
            if (e.getValue().getValueType() == ValueType.VariantRef) {
                e.getValue().setVarIndex(++varIndex);
            }
        }
    }

    private static void invokeFilter(Map<Integer, ColumnInfo> cols, Map<Integer, ColumnInfo> setCols, List<FilterItem> items, Object filter, Map<Integer, Object> parameters) {
        for (val item : items) {
            invokeFilter(cols, setCols, item, filter, parameters);
        }
    }

    @SneakyThrows
    private static void invokeFilter(Map<Integer, ColumnInfo> cols, Map<Integer, ColumnInfo> setCols, FilterItem item, Object filter, Map<Integer, Object> parameters) {
        Method method = item.getMethod();

        List<Object> args = Lists.newArrayList();

        int beanIndex = 0;
        for (val parameter : method.getParameters()) {
            if (parameter.getType() == SqlFilterContext.class) {
                args.add(new SqlFilterContext());
            } else {
                if (beanIndex == 0) {
                    args.add(createBean(parameter, cols, parameters));
                    beanIndex = 1;
                } else if (beanIndex == 1 && setCols != null) {
                    args.add(createBean(parameter, setCols, parameters));
                    beanIndex = 2;
                }
            }
        }

        method.invoke(filter, args.toArray(new Object[args.size()]));
    }

    @SneakyThrows
    private static Object createBean(Parameter parameter, Map<Integer, ColumnInfo> cols, Map<Integer, Object> parameters) {
        Class<?> parameterType = parameter.getType();
        val param = Reflect.on(parameterType).create().get();

        int mapped = 0;

        for (val field : parameterType.getDeclaredFields()) {
            val allowedNames = createAllowedNames(field.getName());

            val columnInfo = findColumn(cols, allowedNames);
            if (columnInfo == null) continue;

            setAccessible(field);

            if (columnInfo.getValueType() == ValueType.VariantRef) {
                field.set(param, parameters.get(columnInfo.getVarIndex()));
            } else if (columnInfo.getValueType() == ValueType.TextLiteral) {
                field.set(param, columnInfo.getValue());
            }

            setMapped(parameterType, param, field.getName());
            ++mapped;
        }

        setNoneMapped(parameterType, param, mapped == 0);

        return param;
    }

    private static void setAccessible(Field field) {
        if (!field.isAccessible()) field.setAccessible(true);
    }

    private static void setNoneMapped(Class<?> parameterType, Object param, boolean noneMapped) throws IllegalAccessException {
        val mappedField = findField(parameterType, "noneMapped");
        if (mappedField == null) return;

        setAccessible(mappedField);
        mappedField.set(param, noneMapped);
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
            ++index;
            val col = cols.get(index);
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
                val simpleName = ((SQLIdentifierExpr) col).getSimpleName();
                cols.put(index, new ColumnInfo(simpleName.toUpperCase()));
            }
        }

        return cols;
    }
}