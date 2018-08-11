package com.github.bingoohuang.sqlfilter;

import com.google.common.collect.Lists;
import lombok.Getter;

import java.util.List;

@Getter
public class ScheduleAddFilter {
    private final List<Schedule> addedSchedules = Lists.newArrayList();

    @SqlFilter(table = "t_schedule", type = FilterType.INSERT)
    public void onScheduleAdd(Schedule schedule, SqlFilterContext context) {
        addedSchedules.add(schedule);
    }
}
