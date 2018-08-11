package com.github.bingoohuang.sqlfilter;

import com.google.common.collect.Lists;
import lombok.*;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class SqlFilterProxyTest {
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Schedule {
        private String id;
        private boolean idMapped;
        private String name;
        private boolean nameMapped;

        private String scheduleState;
    }

    public static class ScheduleAddFilter {
        @Getter private List<Schedule> schedules = Lists.newArrayList();

        @SqlFilter(table = "t_schedule", type = FilterType.INSERT)
        public void onScheduleAdd(Schedule schedule, SqlFilterContext context) {
            schedules.add(schedule);
        }

        @SqlFilter(table = "t_schedule", type = FilterType.UPDATE)
        public void onScheduleUpdate(Schedule scheduleOld, Schedule scheduleNew) {

        }

        @SqlFilter(table = "t_schedule", type = FilterType.DELETE)
        public void onScheduleDelete(Schedule schedule) {

        }
    }

    @Test @SneakyThrows
    public void test() {
        Class.forName("org.h2.Driver");
        @Cleanup
        val conn = DriverManager.getConnection("jdbc:h2:./src/test/resources/test", "sa", "");

        ScheduleAddFilter filter = new ScheduleAddFilter();
        Connection proxied = SqlFilterProxy.create(conn, filter);
        {
            @Cleanup
            val stmt = proxied.prepareStatement("delete from T_SCHEDULE");
            stmt.executeUpdate();
        }

        {
            @Cleanup
            val stmt = proxied.prepareStatement("insert into T_SCHEDULE(id, name, schedule_state) values(?, ?, '正常')");
            executeUpdate(stmt, "1", "bingoo");
            executeUpdate(stmt, "2", "dingoo");
        }

        assertThat(filter.schedules).isEqualTo(Lists.newArrayList(
                Schedule.builder().id("1").idMapped(true).name("bingoo").nameMapped(true).scheduleState("正常").build(),
                Schedule.builder().id("2").idMapped(true).name("dingoo").nameMapped(true).scheduleState("正常").build()
        ));
    }

    @SneakyThrows
    public int executeUpdate(PreparedStatement ps, Object... boundParameters) {
        for (int i = 0; i < boundParameters.length; ++i) {
            ps.setObject(i + 1, boundParameters[i]);
        }

        return ps.executeUpdate();
    }
}