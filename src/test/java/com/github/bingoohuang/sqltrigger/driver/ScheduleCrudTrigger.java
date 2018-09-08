package com.github.bingoohuang.sqltrigger.driver;

import com.github.bingoohuang.sqltrigger.SqlTrigger;
import com.github.bingoohuang.sqltrigger.SqlTriggerContext;
import com.github.bingoohuang.sqltrigger.SqlTriggerAware;
import com.github.bingoohuang.sqltrigger.TriggerType;
import com.github.bingoohuang.sqltrigger.proxy.Schedule;
import com.google.auto.service.AutoService;
import com.google.common.collect.Lists;

import java.util.List;

@AutoService(SqlTriggerAware.class)
public class ScheduleCrudTrigger implements SqlTriggerAware {
    public final static List<Schedule> addedSchedules = Lists.newArrayList();

    @SqlTrigger(table = "t_schedule", type = TriggerType.INSERT)
    public void onScheduleAdd(Schedule schedule) {
        addedSchedules.add(schedule);
    }


    public final static List<Schedule> deletedSchedules = Lists.newArrayList();
    public final static List<Schedule> updatedSchedules = Lists.newArrayList();

    @SqlTrigger(table = "t_schedule", type = TriggerType.UPDATE)
    public void onScheduleUpdate(Schedule scheduleOld, Schedule scheduleNew) {
        updatedSchedules.add(scheduleOld);
        updatedSchedules.add(scheduleNew);
    }

    @SqlTrigger(table = "t_schedule", type = TriggerType.DELETE)
    public void onScheduleDelete(Schedule schedule) {
        deletedSchedules.add(schedule);
    }
}
