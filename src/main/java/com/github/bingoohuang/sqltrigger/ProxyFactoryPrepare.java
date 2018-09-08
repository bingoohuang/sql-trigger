package com.github.bingoohuang.sqltrigger;

public class ProxyFactoryPrepare {
    public boolean requiredProxy() {
        return false;
    }

    public Object createPsProxyFactory(Object ps) {
        return ps;
    }
}
