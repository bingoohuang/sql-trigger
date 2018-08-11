package com.github.bingoohuang.sqlfilter;


import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.druid.util.StringUtils;
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

import static com.github.bingoohuang.sqlfilter.ReflectUtil.setField;

public class SqlFilterProxy {
    public static Connection create(Connection conn, Object filter) {
        val map = createFilterConfigMap(filter);

        return (Connection) proxyConnection(conn, map, filter);
    }

    private static Map<String, FilterVo> createFilterConfigMap(Object filter) {
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
        return map;
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

        return ReflectUtil.invokeMethod(method, conn, args);
    }

    private static Object proxyPreparedStatementUpdate(Method method, Object[] args, Map<String, FilterVo> map, Connection conn, SQLUpdateStatement stmt, Object filter) {
        val tableName = stmt.getTableName().getSimpleName();
        val filterVo = map.get(tableName.toUpperCase());
        if (filterVo == null) return ReflectUtil.invokeMethod(method, conn, args);

        val items = filterVo.findByFilterType(FilterType.UPDATE);
        if (items.isEmpty()) return ReflectUtil.invokeMethod(method, conn, args);

        val setCols = createUpdateColumnInfo(stmt);
        val cols = createWhereColumnInfo(stmt.getWhere());

        val ps = ReflectUtil.invokeMethod(method, conn, args);

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
                fulfilColumnInfo(item.getValue(), col);
                setCols.put(index, col);
            }
        }
        return setCols;
    }

    private static Object proxyPreparedStatementDelete(Method method, Object[] args, Map<String, FilterVo> map, Connection conn, SQLDeleteStatement stmt, Object filter) {
        val tableName = stmt.getTableName().getSimpleName();
        val filterVo = map.get(tableName.toUpperCase());
        if (filterVo == null) return ReflectUtil.invokeMethod(method, conn, args);

        val items = filterVo.findByFilterType(FilterType.DELETE);
        if (items.isEmpty()) return ReflectUtil.invokeMethod(method, conn, args);

        val cols = createWhereColumnInfo(stmt.getWhere());

        val ps = ReflectUtil.invokeMethod(method, conn, args);

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

                    ColumnInfo columnInfo = new ColumnInfo(simpleName);
                    cols.put(colIndex, columnInfo);
                    fulfilColumnInfo(right, columnInfo);
                }
            } else if (Str.anyOf(operator, "AND", "OR")) {
                processWhereItems(left, cols);
                processWhereItems(right, cols);
            }
        }
    }

    private static Object proxyPreparedStatementInsert(Method method, Object[] args, Map<String, FilterVo> map, Connection conn, SQLInsertStatement stmt, Object filter) {
        val tableName = stmt.getTableName().getSimpleName();

        val filterVo = map.get(tableName.toUpperCase());
        if (filterVo == null) return ReflectUtil.invokeMethod(method, conn, args);

        val items = filterVo.findByFilterType(FilterType.INSERT);
        if (items.isEmpty()) return ReflectUtil.invokeMethod(method, conn, args);


        val cols = createSqlInsertColumns(stmt);
        fulfilSqlInsertColumns(stmt, cols);

        val ps = ReflectUtil.invokeMethod(method, conn, args);
        return proxyFilteredPreparedStatement(ps, cols, null, items, filter);

    }

    private static Object proxyFilteredPreparedStatement(Object ps, Map<Integer, ColumnInfo> cols, Map<Integer, ColumnInfo> setCols, List<FilterItem> items, Object filter) {
        fulfilVarIndex(cols, setCols);

        return Proxy.newProxyInstance(SqlFilterProxy.class.getClassLoader(),
                new Class[]{PreparedStatement.class}, new InvocationHandler() {
                    Map<Integer, Object> parameters = null;

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if (Str.anyOf(method.getName(), "setString", "setObject")) {
                            int parameterIndex = (Integer) args[0];
                            if (parameters == null)
                                parameters = Maps.newHashMap();
                            parameters.put(parameterIndex, args[1]);
                        } else if (method.getName().equals("executeUpdate")) {
                            invokeFilter(cols, setCols, items, filter, parameters);
                            parameters = null;
                        }

                        return ReflectUtil.invokeMethod(method, ps, args);
                    }
                });
    }

    private static void fulfilVarIndex(Map<Integer, ColumnInfo> cols, Map<Integer, ColumnInfo> setCols) {
        int varIndex = 0;
        if (setCols != null) {
            varIndex = incrementVariantRef(setCols, varIndex);
        }

        incrementVariantRef(cols, varIndex);
    }

    private static int incrementVariantRef(Map<Integer, ColumnInfo> setCols, int varIndex) {
        for (val e : setCols.entrySet()) {
            if (e.getValue().getValueType() == ValueType.VariantRef) {
                e.getValue().setVarIndex(++varIndex);
            }
        }

        return varIndex;
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

        ReflectUtil.invokeMethod(method, filter, args.toArray(new Object[args.size()]));
    }

    private static Object createBean(Parameter parameter, Map<Integer, ColumnInfo> cols, Map<Integer, Object> parameters) {
        Class<?> parameterType = parameter.getType();
        val param = Reflect.on(parameterType).create().get();

        int mapped = 0;

        for (val field : parameterType.getDeclaredFields()) {
            val allowedNames = createAllowedNames(field);

            val columnInfo = findColumn(cols, allowedNames);
            if (columnInfo == null) continue;

            if (columnInfo.getValueType() == ValueType.VariantRef) {
                setField(field, param, parameters.get(columnInfo.getVarIndex()));
            } else if (columnInfo.getValueType() == ValueType.Literal) {
                setField(field, param, columnInfo.getValue());
            }

            setMapped(parameterType, param, field);
            ++mapped;
        }

        setNoneMapped(parameterType, param, mapped == 0);

        return param;
    }

    private static void setNoneMapped(Class<?> parameterType, Object param, boolean noneMapped) {
        val mappedField = ReflectUtil.findField(parameterType, "noneMapped");
        if (mappedField == null) return;

        setField(mappedField, param, noneMapped);
    }

    private static void setMapped(Class<?> parameterType, Object param, Field field) {
        val filterColumn = field.getAnnotation(SqlFilterColumn.class);
        val mappedFieldName = filterColumn == null || StringUtils.isEmpty(filterColumn.mappedField())
                ? field.getName() + "Mapped" : filterColumn.mappedField();

        val mappedField = ReflectUtil.findField(parameterType, mappedFieldName);
        if (mappedField == null) return;

        setField(mappedField, param, Boolean.TRUE);
    }

    private static ColumnInfo findColumn(Map<Integer, ColumnInfo> cols, Set<String> names) {
        for (val e : cols.entrySet()) {
            if (names.contains(e.getValue().getName())) return e.getValue();
        }

        return null;
    }

    private static Set<String> createAllowedNames(Field field) {
        val filterColumn = field.getAnnotation(SqlFilterColumn.class);
        val fieldName = filterColumn != null ? filterColumn.value() : field.getName();

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

    private static void fulfilSqlInsertColumns(SQLInsertStatement insertStmt, Map<Integer, ColumnInfo> cols) {
        int index = 0;
        for (val value : insertStmt.getValues().getValues()) {
            ++index;
            val col = cols.get(index);
            if (col == null) continue;

            fulfilColumnInfo(value, col);
        }
    }

    private static boolean fulfilColumnInfo(SQLExpr value, ColumnInfo col) {
        if (value instanceof SQLVariantRefExpr) {
            col.setValueType(ValueType.VariantRef);
            return true;
        } else if (value instanceof SQLTextLiteralExpr) {
            col.setValueType(ValueType.Literal);
            val expr = (SQLTextLiteralExpr) value;
            col.setValue(expr.getText());
            return true;
        } else if (value instanceof SQLIntegerExpr) {
            val expr = (SQLIntegerExpr) value;
            col.setValueType(ValueType.Literal);
            col.setValue(expr.getNumber());
            return true;
        }

        return false;
    }

    private static Map<Integer, ColumnInfo> createSqlInsertColumns(SQLInsertStatement insertStmt) {
        Map<Integer, ColumnInfo> cols = Maps.newHashMap();

        int index = 0;
        for (val col : insertStmt.getColumns()) {
            ++index;

            if (col instanceof SQLIdentifierExpr) {
                val expr = (SQLIdentifierExpr) col;
                val simpleName = expr.getSimpleName();
                cols.put(index, new ColumnInfo(simpleName.toUpperCase()));
            }
        }

        return cols;
    }
}