package com.github.bingoohuang.sqltrigger;

import com.google.common.collect.Maps;
import lombok.val;

import java.util.List;
import java.util.Map;

public class SqlTriggerParser {
    Map<String, TriggerBeanVo> map = Maps.newHashMap();

    public SqlTriggerParser(Object... filterBeans) {
        for (val filterBean : filterBeans) {
            for (val method : filterBean.getClass().getMethods()) {
                val t = method.getAnnotation(SqlTrigger.class);
                if (t == null) continue;

                val upperTable = t.table().toUpperCase();
                val filterVo = map.getOrDefault(upperTable, new TriggerBeanVo());
                filterVo.add(t.type(), method);
                map.put(upperTable, filterVo);
            }
        }
    }

    public List<TriggerBeanItem> findByFilterType(String tableName, TriggerType triggerType) {
        val vo = map.get(tableName.toUpperCase());
        if (vo == null) return null;

        return vo.filter(triggerType);
    }
}
