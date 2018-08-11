package com.github.bingoohuang.sqlfilter;

import lombok.val;

public class NameUtil {
    public static String toUpperUnderScore(String fieldName) {
        val sb = new StringBuilder();

        char[] chars = fieldName.toCharArray();
        sb.append(chars[0]);
        for (int i = 1; i < chars.length; ++i) {
            if (Character.isUpperCase(chars[i])) {
                sb.append("_");
            }
            sb.append(chars[i]);
        }

        return sb.toString().toUpperCase();
    }
}
