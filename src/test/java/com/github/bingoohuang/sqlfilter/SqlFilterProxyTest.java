package com.github.bingoohuang.sqlfilter;

import com.google.common.collect.Lists;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class SqlFilterProxyTest {
    @Test @SneakyThrows
    public void test() {
        val filter = new ScheduleFilter();
        @Cleanup val conn = SqlFilterProxy.create(Utils.getH2Connection(), filter);
        Utils.executeUpdate(conn, "delete from T_SCHEDULE");

        {
            val sql = "insert into T_SCHEDULE(id, name, schedule_state) values(?, ?, '正常')";
            @Cleanup val stmt = conn.prepareStatement(sql);
            Utils.executeUpdate(stmt, "1", "bingoo");
            Utils.executeUpdate(stmt, "2", "dingoo");
        }

        assertThat(filter.getAddedSchedules()).isEqualTo(Lists.newArrayList(
                Schedule.builder().id("1").idMapped(true).name("bingoo").nameMapped(true).scheduleState("正常").build(),
                Schedule.builder().id("2").idMapped(true).name("dingoo").nameMapped(true).scheduleState("正常").build()
        ));

        Utils.executeUpdate(conn, "delete from T_SCHEDULE where id = '1'");
        {
            @Cleanup val stmt = conn.prepareStatement("delete from T_SCHEDULE where id = ?");
            Utils.executeUpdate(stmt, "2");
        }

        assertThat(filter.getDeletedSchedules()).isEqualTo(Lists.newArrayList(
                Schedule.builder().noneMapped(true).build(),
                Schedule.builder().id("1").idMapped(true).build(),
                Schedule.builder().id("2").idMapped(true).build()
        ));

        Utils.executeUpdate(conn, "update T_SCHEDULE set name = 'bingoohuang', schedule_state = '失效' where id = '1' and name = ?", "bingoo");
        {
            @Cleanup val stmt = conn.prepareStatement("update T_SCHEDULE set name = ? where id = ?");
            Utils.executeUpdate(stmt, "dingoohuang", "2");
        }

        assertThat(filter.getUpdatedSchedules()).isEqualTo(Lists.newArrayList(
                Schedule.builder().id("1").idMapped(true).name("bingoo").nameMapped(true).build(),
                Schedule.builder().name("bingoohuang").nameMapped(true).scheduleState("失效").build(),
                Schedule.builder().id("2").idMapped(true).build(),
                Schedule.builder().name("dingoohuang").nameMapped(true).build()
        ));
    }

}