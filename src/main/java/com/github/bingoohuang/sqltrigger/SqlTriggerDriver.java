package com.github.bingoohuang.sqltrigger;


import com.google.auto.service.AutoService;
import com.google.common.collect.Lists;
import lombok.val;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

@AutoService(Driver.class)
public class SqlTriggerDriver implements Driver {
    private static Driver INSTANCE = new SqlTriggerDriver();

    static {
        try {
            DriverManager.registerDriver(SqlTriggerDriver.INSTANCE);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not register SqlTriggerDriver with DriverManager", e);
        }
    }

    @Override
    public boolean acceptsURL(final String url) {
        return url != null && url.startsWith("jdbc:sql-trigger:");
    }

    /**
     * Parses out the real JDBC connection URL by removing "p6spy:".
     *
     * @param url the connection URL
     * @return the parsed URL
     */
    private String extractRealUrl(String url) {
        return acceptsURL(url) ? url.replace("sql-trigger:", "") : url;
    }

    static List<Driver> registeredDrivers() {
        List<Driver> result = new ArrayList<>();
        for (Enumeration<Driver> de = DriverManager.getDrivers(); de.hasMoreElements(); ) {
            result.add(de.nextElement());
        }
        return result;
    }

    @Override
    public Connection connect(String url, Properties properties) throws SQLException {
        // if there is no url, we have problems
        if (url == null) throw new SQLException("url is required");
        if (!acceptsURL(url)) return null;

        // find the real driver for the URL
        val passThru = findPassthru(url);

        val conn = passThru.connect(extractRealUrl(url), properties);

        Object[] array = registeredTriggerBeans();
        if (array.length == 0) return conn;

        return new SqlTriggerProxy(array).proxy(conn);
    }

    private Object[] registeredTriggerBeans() {
        List<TriggerBeanAware> beans = Lists.newArrayList();
        for (val beanAware : ServiceLoader.load(TriggerBeanAware.class)) {
            beans.add(beanAware);
        }

        return beans.toArray(new Object[beans.size()]);
    }

    protected Driver findPassthru(String url) throws SQLException {
        for (val driver : registeredDrivers()) {
            try {
                if (driver.acceptsURL(extractRealUrl(url))) {
                    return driver;
                }
            } catch (SQLException e) {
            }
        }

        val realUrl = extractRealUrl(url);
        throw new SQLException("Unable to find a driver that accepts " + realUrl);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties properties) throws SQLException {
        return findPassthru(url).getPropertyInfo(url, properties);
    }

    @Override
    public int getMajorVersion() {
        // This is a bit of a problem since there is no URL to determine the passthru!
        return 2;
    }

    @Override
    public int getMinorVersion() {
        // This is a bit of a problem since there is no URL to determine the passthru!
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        // This is a bit of a problem since there is no URL to determine the passthru!
        return true;
    }

    // Note: @Override annotation not added to allow compilation using Java 1.6
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("Feature not supported");
    }
}