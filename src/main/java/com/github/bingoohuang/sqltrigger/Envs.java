package com.github.bingoohuang.sqltrigger;

public class Envs {
    public static final boolean HAS_SPRING = classExists("org.springframework.context.ApplicationContext");

    public static boolean classExists(String className) {
        try {
            Class.forName(className, false, Envs.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
