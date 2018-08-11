package com.github.bingoohuang.sqlfilter;

import java.sql.Connection;

public interface ProxyPrepare {
    Object create(FilterParser filterParser, Object ps, Object[] filterBeans);
}
