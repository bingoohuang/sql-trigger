package com.github.bingoohuang.sqltrigger;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.val;

import java.util.List;
import java.util.Map;

public class SqlTriggerBeanMap {
    @Getter private final Object[] triggerBeans;
    Map<String, TriggerBeanVo> map = Maps.newHashMap();

    public SqlTriggerBeanMap(Object[] triggerBeans) {
        this.triggerBeans = triggerBeans;

        for (val filterBean : triggerBeans) {
            parseSqlTriggerBean(filterBean);
        }
    }

    private void parseSqlTriggerBean(Object filterBean) {
        for (val method : filterBean.getClass().getMethods()) {
            val t = method.getAnnotation(SqlTrigger.class);
            if (t == null) continue;

            for (val table : t.table()) {
                val ut = table.toUpperCase();
                val filterVo = map.getOrDefault(ut, new TriggerBeanVo());
                filterVo.add(t.type(), method);
                map.put(ut, filterVo);
            }
        }
    }

    public List<TriggerBeanItem> findByTriggerType(String tableName, TriggerType triggerType) {
        val vo = map.get(tableName.toUpperCase());
        if (vo == null) return null;

        return vo.filter(triggerType);
    }

    public boolean isEmpty() {
        return triggerBeans.length == 0;
    }
}
