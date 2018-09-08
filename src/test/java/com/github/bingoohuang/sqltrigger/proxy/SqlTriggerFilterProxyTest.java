package com.github.bingoohuang.sqltrigger.proxy;

import com.github.bingoohuang.sqltrigger.SqlTriggerProxy;
import com.github.bingoohuang.utils.lang.Mapp;
import com.google.common.collect.Lists;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.github.bingoohuang.sqltrigger.proxy.SqlTypeTestUtil.*;
import static com.google.common.truth.Truth.assertThat;

public class SqlTriggerFilterProxyTest {
    @Test @SneakyThrows
    public void test() {
        val addFilter = new ScheduleAddFilter();
        val filter = new ScheduleFilter();

        val h2Conn = SqlTypeTestUtil.getH2Connection();
        executeSqls(h2Conn, "drop table  if exists T_SCHEDULE_PROXY",
                "create table T_SCHEDULE_PROXY ( id varchar(20) primary key, name varchar(20) not null, schedule_state varchar(3) not null comment '状态', subscribes int not null default 0 comment '订课人数' )");


        @Cleanup val conn = new SqlTriggerProxy(filter, addFilter).proxy(h2Conn);
        executeUpdate(conn, "delete from T_SCHEDULE_PROXY");
        assertThat(filter.getDeletedSchedules()).isEqualTo(Lists.newArrayList(
                Schedule.builder().noneMapped(true).build()));

        executeUpdate(conn, "insert into T_SCHEDULE_PROXY(id, name, schedule_state, subscribes) values(?, ?, '正常', ?), (?, ?, '正常', ?)",
                "10", "xxx", 10, "20", "yyy", 20);
        assertThat(addFilter.getAddedSchedules()).isEqualTo(Lists.newArrayList(
                Schedule.builder().id("10").idMapped(true).name("xxx").nameMapped(true).state("正常").stateUsed(true).subscribes(10).build(),
                Schedule.builder().id("20").idMapped(true).name("yyy").nameMapped(true).state("正常").stateUsed(true).subscribes(20).build()
        ));

        executeUpdate(conn, "delete from T_SCHEDULE_PROXY");
        addFilter.getAddedSchedules().clear();
        {
            val sql = "insert into T_SCHEDULE_PROXY(id, name, schedule_state, subscribes) values(?, ?, '正常', 9)";
            @Cleanup val stmt = conn.prepareStatement(sql);
            executeUpdate(stmt, "1", "bingoo");
            executeUpdate(stmt, "2", "dingoo");
        }

        assertThat(addFilter.getAddedSchedules()).isEqualTo(Lists.newArrayList(
                Schedule.builder().id("1").idMapped(true).name("bingoo").nameMapped(true).state("正常").stateUsed(true).subscribes(9).build(),
                Schedule.builder().id("2").idMapped(true).name("dingoo").nameMapped(true).state("正常").stateUsed(true).subscribes(9).build()
        ));

        executeUpdate(conn, "update T_SCHEDULE_PROXY set name = 'bingoohuang', schedule_state = '失效' where id = '1' and name = ?", "bingoo");
        {
            @Cleanup val stmt = conn.prepareStatement("update T_SCHEDULE_PROXY set name = ? where id = ?");
            executeUpdate(stmt, "dingoohuang", "2");
        }

        assertThat(filter.getUpdatedSchedules()).isEqualTo(Lists.newArrayList(
                Schedule.builder().id("1").idMapped(true).name("bingoo").nameMapped(true).build(),
                Schedule.builder().name("bingoohuang").nameMapped(true).state("失效").stateUsed(true).build(),
                Schedule.builder().id("2").idMapped(true).build(),
                Schedule.builder().name("dingoohuang").nameMapped(true).build()
        ));

        List<Map<String, Object>> list = executeSelect(conn, "select id, name, schedule_state from T_SCHEDULE_PROXY");

        assertThat(list).isEqualTo(Lists.newArrayList(
                Mapp.of("ID", "1", "NAME", "bingoohuang", "SCHEDULE_STATE", "失效"),
                Mapp.of("ID", "2", "NAME", "dingoohuang", "SCHEDULE_STATE", "正常")
        ));

        filter.getDeletedSchedules().clear();
        executeUpdate(conn, "delete from T_SCHEDULE_PROXY where id = '1'");
        {
            @Cleanup val stmt = conn.prepareStatement("delete from T_SCHEDULE_PROXY where id = ?");
            executeUpdate(stmt, "2");
        }

        assertThat(filter.getDeletedSchedules()).isEqualTo(Lists.newArrayList(
                Schedule.builder().id("1").idMapped(true).build(),
                Schedule.builder().id("2").idMapped(true).build()
        ));
    }

}