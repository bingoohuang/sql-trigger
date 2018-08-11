package com.github.bingoohuang.sqlfilter;

import com.google.common.collect.Lists;
import lombok.Getter;

import java.util.List;

public class ScheduleFilter {
    @Getter private List<Schedule> addedSchedules = Lists.newArrayList();
    @Getter private List<Schedule> deletedSchedules = Lists.newArrayList();

    @SqlFilter(table = "t_schedule", type = FilterType.INSERT)
    public void onScheduleAdd(Schedule schedule, SqlFilterContext context) {
        addedSchedules.add(schedule);
    }

    @SqlFilter(table = "t_schedule", type = FilterType.UPDATE)
    public void onScheduleUpdate(Schedule scheduleOld, Schedule scheduleNew) {

    }

    @SqlFilter(table = "t_schedule", type = FilterType.DELETE)
    public void onScheduleDelete(Schedule schedule) {
        deletedSchedules.add(schedule);
    }
}
