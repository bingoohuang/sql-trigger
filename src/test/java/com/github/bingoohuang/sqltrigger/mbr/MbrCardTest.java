package com.github.bingoohuang.sqltrigger.mbr;

import com.github.bingoohuang.sqltrigger.SqlTriggerProxy;
import com.github.bingoohuang.sqltrigger.proxy.SqlTypeTestUtil;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Test;

import static com.github.bingoohuang.sqltrigger.proxy.SqlTypeTestUtil.executeSqls;
import static com.github.bingoohuang.sqltrigger.proxy.SqlTypeTestUtil.executeUpdate;
import static com.google.common.truth.Truth.assertThat;

public class MbrCardTest {
    @Test @SneakyThrows
    public void test() {
        val trigger = new MbrCardTrigger();

        val h2Conn = SqlTypeTestUtil.getH2Connection();
        executeSqls(h2Conn, "drop table if exists T_SCHEDULE_PROXY",
                "create table T_SCHEDULE_PROXY ( id varchar(20) primary key, name varchar(20) not null, schedule_state varchar(3) not null comment '状态', subscribes int not null default 0 comment '订课人数' )");

        @Cleanup val conn = new SqlTriggerProxy(trigger).proxy(h2Conn);

        executeSqls(conn, "drop table if exists TT_F_MBR_CARD",
                "create table TT_F_MBR_CARD ( MBR_CARD_ID varchar(20) primary key, AVAIL_AMOUNT int not null, UPDATE_TIME datetime not null, LATEST_ACTIVATE_DAY datetime)");


        executeUpdate(conn, "UPDATE TT_F_MBR_CARD MC SET MC.AVAIL_AMOUNT = MC.AVAIL_AMOUNT - ?, MC.UPDATE_TIME = CURRENT_TIMESTAMP() WHERE MC.MBR_CARD_ID = ?", 2, "193139773302243329");

        assertThat(trigger.getOld().getMbrCardId()).isEqualTo("193139773302243329");
        assertThat(trigger.getOld().isLatestActivateDayMapped()).isFalse();

        executeUpdate(conn, "UPDATE TT_F_MBR_CARD MC SET LATEST_ACTIVATE_DAY = null WHERE MC.MBR_CARD_ID = ?", "193139773302243329");

        assertThat(trigger.getOld().getMbrCardId()).isEqualTo("193139773302243329");
        assertThat(trigger.getNewOne().getLatestActivateDay()).isNull();
        assertThat(trigger.getNewOne().isLatestActivateDayMapped()).isTrue();
    }
}
