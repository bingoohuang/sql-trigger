package com.github.bingoohuang.sqltrigger.proxy;

import com.github.bingoohuang.sqltrigger.SqlTrigger;
import com.github.bingoohuang.sqltrigger.TriggerType;
import com.google.common.collect.Lists;
import lombok.Getter;

import java.util.List;

@Getter
public class ScheduleFilter {
    private final List<Schedule> deletedSchedules = Lists.newArrayList();
    private final List<Schedule> updatedSchedules = Lists.newArrayList();

    @SqlTrigger(table = "T_SCHEDULE_PROXY", type = TriggerType.UPDATE)
    public void onScheduleUpdate(Schedule scheduleOld, Schedule scheduleNew) {
        updatedSchedules.add(scheduleOld);
        updatedSchedules.add(scheduleNew);
    }

    @SqlTrigger(table = "T_SCHEDULE_PROXY", type = TriggerType.DELETE)
    public void onScheduleDelete(Schedule schedule) {
        deletedSchedules.add(schedule);
    }
}
