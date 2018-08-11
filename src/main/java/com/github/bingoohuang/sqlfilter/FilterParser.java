package com.github.bingoohuang.sqlfilter;

import com.google.common.collect.Maps;
import lombok.val;

import java.util.List;
import java.util.Map;

public class FilterParser {
    Map<String, FilterVo> map = Maps.newHashMap();

    public FilterParser(Object... filterBeans) {
        for (val filterBean : filterBeans) {
            for (val method : filterBean.getClass().getMethods()) {
                val sqlFilter = method.getAnnotation(SqlFilter.class);
                if (sqlFilter == null) continue;

                val upperTable = sqlFilter.table().toUpperCase();
                val filterVo = map.getOrDefault(upperTable, new FilterVo());
                filterVo.add(sqlFilter.type(), method);
                map.put(upperTable, filterVo);
            }
        }
    }

    public List<FilterItem> findByFilterType(String tableName, FilterType filterType) {
        val filterVo = map.get(tableName.toUpperCase());
        if (filterVo == null) return null;

        return filterVo.filter(filterType);
    }
}
