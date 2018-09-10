package com.github.bingoohuang.sqltrigger.mbr;

import com.github.bingoohuang.sqltrigger.SqlTrigger;
import com.github.bingoohuang.sqltrigger.SqlTriggerAware;
import com.github.bingoohuang.sqltrigger.TriggerType;
import com.google.auto.service.AutoService;
import lombok.Getter;

@AutoService(SqlTriggerAware.class)
public class MbrCardTrigger implements SqlTriggerAware {
    @Getter private MbrCardLastActivateDayVo old;
    @Getter private MbrCardLastActivateDayVo newOne;

    /**
     * 更新会员卡时，增加最迟激活日期任务。
     */
    @SqlTrigger(table = "TT_F_MBR_CARD", type = TriggerType.UPDATE)
    public void update(MbrCardLastActivateDayVo old, MbrCardLastActivateDayVo newOne) {
        this.old = old;
        this.newOne = newOne;
    }
}
