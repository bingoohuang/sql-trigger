# sql-trigger
[![Build Status](https://travis-ci.org/bingoohuang/sql-trigger.svg?branch=master)](https://travis-ci.org/bingoohuang/sql-trigger)
[![Coverage Status](https://coveralls.io/repos/github/bingoohuang/sql-trigger/badge.svg?branch=master)](https://coveralls.io/github/bingoohuang/sql-trigger?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.bingoohuang/sql-trigger/badge.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/com.github.bingoohuang/sql-trigger/)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

jdbc sql filter to sql postprocessing like sql rewriting, crud trigger and etc.

# Intention
At some cases, we want to trigger extra actions when add/update/delete in some specified tables, like tt_f_mbr_card which stands for member cards, 
but may be those sqls will be invoked by java code in  more than once places. We won't to add extra code processing at those more than once places.
So I create this little java library to intercept from the connection source to inject some trigger actions.

# Usage

1. Add dependency to pom.xml.
    ```xml
    <dependency>
        <groupId>com.github.bingoohuang</groupId>
        <artifactId>sql-trigger</artifactId>
        <version>0.0.2-SNAPSHOT</version>
    </dependency>
    ``` 

1. Use SqlTriggerDriver, just replace jdbc url like `jdbc:h2:./src/test/resources/test` to `jdbc:sqltrigger:h2:./src/test/resources/test`.
1. Or manually coding like:
    ```java
    private SqlTriggerProxy sqlTriggerProxy = SqlTriggerProxy.createByRegisteredTriggerBeans();
    
    public Connection getConnection() {
        val connection = dataSource.getConnection();
        return sqlTriggerProxy.proxy(connection);
    }
    ```

1. Register the trigger like
    ```java
    @Data
    public class MbrCardLastActivateDayVo {
        private String mbrCardId;
        private Object latestActivateDay;
    }
    
    @AutoService(SqlTriggerAware.class)
    public class MbrCardTrigger implements SqlTriggerAware {
        /**
         * 新增会员卡时，增加最迟激活日期任务。
         */
        @SqlTrigger(table = {"TT_F_MBR_CARD", "TG_F_MEMBER_CARD"}, type = TriggerType.INSERT)
        public void insert(MbrCardLastActivateDayVo vo) {
            val day = vo.getLatestActivateDay();
            val task = Springs.getBean(MemberCardLatestActivateTask.class);
            val dt = day instanceof DateTime ? (DateTime) day : DateTimes.parse((String) day);
            task.submit(vo.getMbrCardId(), dt);
        }
    
        /**
         * 更新会员卡时，增加最迟激活日期任务。
         */
        @SqlTrigger(table = {"TT_F_MBR_CARD", "TG_F_MEMBER_CARD"}, type = TriggerType.UPDATE)
        public void update(MbrCardLastActivateDayVo old, MbrCardLastActivateDayVo newOne) {
            val day = newOne.getLatestActivateDay();
            val mbrCardId = old.getMbrCardId();
    
            val dt = day instanceof DateTime ? (DateTime) day : DateTimes.parse((String) day);
            val oldCard = Springs.getBean(TtfMemberCardDao.class).findMemberCard(mbrCardId);
            // 最迟激活日期没有变化时，不处理
            if (DateTimes.isSameDay(oldCard.getLatestActivateDay(), dt)) return;
    
            val task = Springs.getBean(MemberCardLatestActivateTask.class);
            task.cancel(mbrCardId);
            task.submit(mbrCardId, dt);
        }
    }
    ```

1. Then the insert/update sql like the following will be caught to trigger the methods of auto-serviced class.
```sql
insert into TT_F_MBR_CARD(mbr_card_id, latest_activiate_day) values(?, ?);

update TT_F_MBR_CARD set latest_activiate_day = ? where mbr_card_id = ?;
```  
    