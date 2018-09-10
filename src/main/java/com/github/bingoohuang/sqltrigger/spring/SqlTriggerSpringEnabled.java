package com.github.bingoohuang.sqltrigger.spring;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(SqlTriggerSpringConfig.class)
public @interface SqlTriggerSpringEnabled {

}
