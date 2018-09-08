package com.github.bingoohuang.sqltrigger;

import lombok.val;

public class NameUtil {
    public static String toUpperUnderScore(String name) {
        val sb = new StringBuilder();

        char[] chars = name.toCharArray();
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
