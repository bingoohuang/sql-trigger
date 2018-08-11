package com.github.bingoohuang.sqlfilter;

import lombok.SneakyThrows;
import lombok.val;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectUtil {
    @SneakyThrows
    public static Object invokeMethod(Method method, Object obj, Object... args) {
        return method.invoke(obj, args);
    }

    @SneakyThrows
    public static void setField(Field field, Object obj, Object value) {
        if (!field.isAccessible()) field.setAccessible(true);
        field.set(obj, value);
    }

    public static Field findField(Class<?> type, String fieldName) {
        for (val field : type.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) return field;
        }

        return null;
    }
}
