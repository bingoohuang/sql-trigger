package com.github.bingoohuang.sqltrigger.mbr;

import lombok.Data;

@Data
public class MbrCardLastActivateDayVo {
    private String mbrCardId;
    private Object latestActivateDay;
    private boolean latestActivateDayMapped;
}
