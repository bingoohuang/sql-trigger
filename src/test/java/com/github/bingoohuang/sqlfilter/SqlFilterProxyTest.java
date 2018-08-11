package com.github.bingoohuang.sqlfilter;

import com.github.bingoohuang.utils.lang.Mapp;
import com.google.common.collect.Lists;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.github.bingoohuang.sqlfilter.SqlTypeTestUtil.executeSelect;
import static com.github.bingoohuang.sqlfilter.SqlTypeTestUtil.executeUpdate;
import static com.google.common.truth.Truth.assertThat;

public class SqlFilterProxyTest {
    @Test @SneakyThrows
    public void test() {
        val addFilter = new ScheduleAddFilter();
        val filter = new ScheduleFilter();

        @Cleanup val conn = new SqlFilterProxy(filter, addFilter).proxy(SqlTypeTestUtil.getH2Connection());
        executeUpdate(conn, "delete from T_SCHEDULE");
        assertThat(filter.getDeletedSchedules()).isEqualTo(Lists.newArrayList(
                Schedule.builder().noneMapped(true).build()));

        executeUpdate(conn, "insert into T_SCHEDULE(id, name, schedule_state, subscribes) values(?, ?, '正常', ?), (?, ?, '正常', ?)",
                "10", "xxx", 10, "20", "yyy", 20);
        assertThat(addFilter.getAddedSchedules()).isEqualTo(Lists.newArrayList(
                Schedule.builder().id("10").idMapped(true).name("xxx").nameMapped(true).state("正常").stateUsed(true).subscribes(10).build(),
                Schedule.builder().id("20").idMapped(true).name("yyy").nameMapped(true).state("正常").stateUsed(true).subscribes(20).build()
        ));

        executeUpdate(conn, "delete from T_SCHEDULE");
        addFilter.getAddedSchedules().clear();
        {
            val sql = "insert into T_SCHEDULE(id, name, schedule_state, subscribes) values(?, ?, '正常', 9)";
            @Cleanup val stmt = conn.prepareStatement(sql);
            executeUpdate(stmt, "1", "bingoo");
            executeUpdate(stmt, "2", "dingoo");
        }

        assertThat(addFilter.getAddedSchedules()).isEqualTo(Lists.newArrayList(
                Schedule.builder().id("1").idMapped(true).name("bingoo").nameMapped(true).state("正常").stateUsed(true).subscribes(9).build(),
                Schedule.builder().id("2").idMapped(true).name("dingoo").nameMapped(true).state("正常").stateUsed(true).subscribes(9).build()
        ));

        executeUpdate(conn, "update T_SCHEDULE set name = 'bingoohuang', schedule_state = '失效' where id = '1' and name = ?", "bingoo");
        {
            @Cleanup val stmt = conn.prepareStatement("update T_SCHEDULE set name = ? where id = ?");
            executeUpdate(stmt, "dingoohuang", "2");
        }

        assertThat(filter.getUpdatedSchedules()).isEqualTo(Lists.newArrayList(
                Schedule.builder().id("1").idMapped(true).name("bingoo").nameMapped(true).build(),
                Schedule.builder().name("bingoohuang").nameMapped(true).state("失效").stateUsed(true).build(),
                Schedule.builder().id("2").idMapped(true).build(),
                Schedule.builder().name("dingoohuang").nameMapped(true).build()
        ));

        List<Map<String, Object>> list = executeSelect(conn, "select id, name, schedule_state from T_SCHEDULE");

        assertThat(list).isEqualTo(Lists.newArrayList(
                Mapp.of("ID", "1", "NAME", "bingoohuang", "SCHEDULE_STATE", "失效"),
                Mapp.of("ID", "2", "NAME", "dingoohuang", "SCHEDULE_STATE", "正常")
        ));

        filter.getDeletedSchedules().clear();
        executeUpdate(conn, "delete from T_SCHEDULE where id = '1'");
        {
            @Cleanup val stmt = conn.prepareStatement("delete from T_SCHEDULE where id = ?");
            executeUpdate(stmt, "2");
        }

        assertThat(filter.getDeletedSchedules()).isEqualTo(Lists.newArrayList(
                Schedule.builder().id("1").idMapped(true).build(),
                Schedule.builder().id("2").idMapped(true).build()
        ));
    }

}