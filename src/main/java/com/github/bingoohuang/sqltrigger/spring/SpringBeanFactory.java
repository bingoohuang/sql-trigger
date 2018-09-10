package com.github.bingoohuang.sqltrigger.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class SpringBeanFactory implements ApplicationContextAware {
    private static ApplicationContext appContext;

    public static <T> Collection<T> getBeans(Class<T> clazz) {
        return appContext.getBeansOfType(clazz).values();
    }

    @Override public void setApplicationContext(ApplicationContext appContext) {
        this.appContext = appContext;
    }

    public static boolean isSpringEnabled() {
        return appContext != null;
    }
}
