package com.github.bingoohuang.sqltrigger.proxy;

import com.github.bingoohuang.sqltrigger.SqlTrigger;
import com.github.bingoohuang.sqltrigger.SqlTriggerContext;
import com.github.bingoohuang.sqltrigger.TriggerType;
import com.google.common.collect.Lists;
import lombok.Getter;

import java.util.List;

@Getter
public class ScheduleAddFilter {
    private final List<Schedule> addedSchedules = Lists.newArrayList();

    @SqlTrigger(table = "T_SCHEDULE_PROXY", type = TriggerType.INSERT)
    public void onScheduleAdd(Schedule schedule, SqlTriggerContext context) {
        addedSchedules.add(schedule);
    }
}
