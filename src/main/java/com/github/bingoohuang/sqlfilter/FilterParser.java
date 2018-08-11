package com.github.bingoohuang.sqlfilter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.val;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class FilterParser {
    Map<String, FilterVo> map = Maps.newHashMap();

    public FilterParser(Object filter) {
        Method[] methods = filter.getClass().getMethods();
        for (val method : methods) {
            val sqlFilter = method.getAnnotation(SqlFilter.class);
            if (sqlFilter == null) continue;

            val upperCaseTable = sqlFilter.table().toUpperCase();
            FilterVo filterVo = map.get(upperCaseTable);
            if (filterVo == null) {
                filterVo = new FilterVo();
                map.put(upperCaseTable, filterVo);
            }

            filterVo.add(sqlFilter.type(), method);
        }
    }

    public List<FilterItem> findByFilterType(String tableName, FilterType filterType) {
        val filterVo = map.get(tableName.toUpperCase());
        if (filterVo == null) return Lists.newArrayList();

        return filterVo.findByFilterType(filterType);
    }
}
