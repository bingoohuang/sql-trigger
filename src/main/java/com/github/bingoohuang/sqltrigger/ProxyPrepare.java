package com.github.bingoohuang.sqltrigger;

public interface ProxyPrepare {
    Object create(SqlTriggerParser sqlTriggerParser, Object ps, Object[] filterBeans);
}
