package com.github.bingoohuang.sqltrigger;


import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.util.JdbcConstants;
import lombok.val;

import java.lang.reflect.Proxy;
import java.sql.Connection;

import static com.github.bingoohuang.sqltrigger.ReflectUtil.invokeMethod;

public class SqlTriggerProxy {
    private final Object[] triggerBeans;
    private final SqlTriggerParser sqlTriggerParser;

    public SqlTriggerProxy(Object... triggerBeans) {
        this.triggerBeans = triggerBeans;
        this.sqlTriggerParser = new SqlTriggerParser(triggerBeans);
    }

    public Connection proxy(Connection conn) {
        return (Connection) Proxy.newProxyInstance(SqlTriggerProxy.class.getClassLoader(),
                new Class[]{Connection.class}, (proxy, method, args) -> {
                    val invoke = invokeMethod(method, conn, args);
                    if (method.getName().equals("prepareStatement")) {
                        return proxyPreparedStatement(invoke, args);
                    }

                    return invoke;
                });
    }

    private Object proxyPreparedStatement(Object ps, Object[] args) {
        val stmts = SQLUtils.parseStatements((String) args[0], JdbcConstants.MYSQL);
        val parser = createFilterSqlParser(stmts.get(0));
        return parser != null ? parser.create(sqlTriggerParser, ps, triggerBeans) : ps;
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