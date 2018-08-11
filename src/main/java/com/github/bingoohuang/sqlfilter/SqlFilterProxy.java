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
    private final Object[] filterBeans;
    private final FilterParser filterParser;

    public SqlFilterProxy(Connection conn, Object... filterBeans) {
        this.conn = conn;
        this.filterBeans = filterBeans;
        this.filterParser = new FilterParser(filterBeans);
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
        val stmts = SQLUtils.parseStatements((String) args[0], JdbcConstants.MYSQL);
        val ps = invokeMethod(method, conn, args);
        val sqlParser = createFilterSqlParser(stmts.get(0));
        return sqlParser != null ? sqlParser.create(filterParser, ps, filterBeans) : ps;
    }

    private ProxyPrepare createFilterSqlParser(SQLStatement stmt) {
        if (stmt instanceof SQLInsertStatement) {
            return new ProxyInsert((SQLInsertStatement) stmt);
        } else if (stmt instanceof SQLDeleteStatement) {
            return new ProxyDelete((SQLDeleteStatement) stmt);
        } else if (stmt instanceof SQLUpdateStatement) {
            return new ProxyUpdate((SQLUpdateStatement) stmt);
        }

        return null;
    }
}