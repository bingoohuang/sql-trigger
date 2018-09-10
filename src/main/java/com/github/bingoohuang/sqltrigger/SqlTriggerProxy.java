package com.github.bingoohuang.sqltrigger;


import com.github.bingoohuang.sqltrigger.spring.SpringBeanFactory;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.List;
import java.util.ServiceLoader;

import static com.github.bingoohuang.sqltrigger.ReflectUtil.invokeMethod;

@Slf4j
public class SqlTriggerProxy {
    private final ProxyFactoryCache proxyFactoryCache;

    public SqlTriggerProxy(Object... beans) {
        this.proxyFactoryCache = new ProxyFactoryCache(new SqlTriggerBeanMap(beans));
    }

    public static Object[] registeredTriggerBeans() {
        List<SqlTriggerAware> beans = Lists.newArrayList();

        if (Envs.HAS_SPRING && SpringBeanFactory.isSpringEnabled()) {
            beans.addAll(SpringBeanFactory.getBeans(SqlTriggerAware.class));
        } else {
            for (val aware : ServiceLoader.load(SqlTriggerAware.class)) {
                beans.add(aware);
            }
        }

        return beans.toArray(new Object[beans.size()]);
    }

    public static SqlTriggerProxy createByRegisteredTriggerBeans() {
        return new SqlTriggerProxy(registeredTriggerBeans());
    }

    public Connection proxy(Connection conn) {
        if (!proxyFactoryCache.hasTriggerBean()) return conn;

        return (Connection) Proxy.newProxyInstance(SqlTriggerProxy.class.getClassLoader(),
                new Class[]{Connection.class}, (proxy, method, args) -> {
                    val invoke = invokeMethod(method, conn, args);
                    if (method.getName().equals("prepareStatement")) {
                        return proxyPs(invoke, args);
                    }

                    return invoke;
                });
    }

    private Object proxyPs(Object ps, Object[] args) {
        try {
            val proxyFactory = proxyFactoryCache.getProxyFactory((String) args[0]);
            return proxyFactory.requiredProxy() ? proxyFactory.createPsProxyFactory(ps) : ps;
        } catch (Exception ex) {
            log.warn("fail to parse sql {}", args[0]);
            return ps;
        }
    }

}