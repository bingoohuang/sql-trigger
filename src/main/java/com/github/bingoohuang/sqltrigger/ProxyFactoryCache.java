package com.github.bingoohuang.sqltrigger;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.util.JdbcConstants;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class ProxyFactoryCache {
    private final SqlTriggerBeanMap beanMap;
    private LoadingCache<String, ProxyFactoryPrepare> cache = Caffeine.newBuilder().build(sql -> load(sql));

    private ProxyFactoryPrepare load(String sql) {
        val sts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        val proxyFactory = createProxyFactory(sql, sts.get(0));
        return proxyFactory;
    }

    private ProxyFactoryPrepare createProxyFactory(String sql, SQLStatement s) {
        if (s instanceof SQLInsertStatement) return new ProxyFactoryInsert(sql, beanMap, (SQLInsertStatement) s);
        if (s instanceof SQLDeleteStatement) return new ProxyFactoryDelete(sql, beanMap, (SQLDeleteStatement) s);
        if (s instanceof SQLUpdateStatement) return new ProxyFactoryUpdate(sql, beanMap, (SQLUpdateStatement) s);

        return new ProxyFactoryPrepare();
    }

    public boolean hasTriggerBean() {
        return !beanMap.isEmpty();
    }

    public ProxyFactoryPrepare getProxyFactory(String sql) {
        return cache.get(sql);
    }
}
