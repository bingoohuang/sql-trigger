package com.github.bingoohuang.sqlfilter;


import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.util.JdbcConstants;
import lombok.val;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

import static com.github.bingoohuang.sqlfilter.ReflectUtil.invokeMethod;

public class SqlFilterProxy {
    private final Connection conn;
    private final Object filter;
    private final FilterParser filterParser;

    public SqlFilterProxy(Connection conn, Object filter) {
        this.conn = conn;
        this.filter = filter;
        this.filterParser = new FilterParser(filter);
    }

    public Connection create() {
        return (Connection) Proxy.newProxyInstance(SqlFilterProxy.class.getClassLoader(),
                new Class[]{Connection.class}, (proxy, method, args) -> {
                    if (method.getName().equals("prepareStatement")) {
                        return proxyPreparedStatement(method, args);
                    }

                    return method.invoke(conn, args);
                });
    }

    private Object proxyPreparedStatement(Method method, Object[] args) {
        val sqlStatements = SQLUtils.parseStatements((String) args[0], JdbcConstants.MYSQL);
        val filterSqlParser = createFilterSqlParser(method, args, sqlStatements.get(0));
        if (filterSqlParser != null) {
            return filterSqlParser.create(filterParser, conn, filter);
        }

        return invokeMethod(method, conn, args);
    }

    private ProxyPrepare createFilterSqlParser(Method method, Object[] args, SQLStatement stmt) {
        if (stmt instanceof SQLInsertStatement) {
            return new ProxyInsert((SQLInsertStatement) stmt, method, args);
        } else if (stmt instanceof SQLDeleteStatement) {
            return new ProxyDelete((SQLDeleteStatement) stmt, method, args);
        } else if (stmt instanceof SQLUpdateStatement) {
            return new ProxyUpdate((SQLUpdateStatement) stmt, method, args);
        }

        return null;
    }
}