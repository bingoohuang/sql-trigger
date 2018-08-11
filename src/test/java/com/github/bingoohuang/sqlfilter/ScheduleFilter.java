package com.github.bingoohuang.sqlfilter;

import com.google.common.collect.Lists;
import lombok.Getter;

import java.util.List;

@Getter
public class ScheduleFilter {
    private final List<Schedule> deletedSchedules = Lists.newArrayList();
    private final List<Schedule> updatedSchedules = Lists.newArrayList();

    @SqlFilter(table = "t_schedule", type = FilterType.UPDATE)
    public void onScheduleUpdate(Schedule scheduleOld, Schedule scheduleNew) {
        updatedSchedules.add(scheduleOld);
        updatedSchedules.add(scheduleNew);
    }

    @SqlFilter(table = "t_schedule", type = FilterType.DELETE)
    public void onScheduleDelete(Schedule schedule) {
        deletedSchedules.add(schedule);
    }
}
