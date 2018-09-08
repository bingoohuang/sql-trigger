# sql-trigger
[![Build Status](https://travis-ci.org/bingoohuang/sql-trigger.svg?branch=master)](https://travis-ci.org/bingoohuang/sql-trigger)
[![Coverage Status](https://coveralls.io/repos/github/bingoohuang/sql-trigger/badge.svg?branch=master)](https://coveralls.io/github/bingoohuang/sql-trigger?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.bingoohuang/sql-trigger/badge.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/com.github.bingoohuang/sql-trigger/)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

jdbc sql filter to sql postprocessing like sql rewriting, crud trigger and etc.

# Intention
At some cases, we want to trigger extra actions when do add/update/delete in some specified tables, like tt_f_mbr_card which stands for member cards, 
but java code which execute those sqls will be invoked in more than once places. We won't to copy and paste extra processing here and there.
So I create this tiny library, which can intercept sql executions to trigger some extra actions without adding invoking code more than one places.

一个非常实际的场景是，排课，我们需要在排课（增加/复制/修改/删除）的时候，添加额外处理：
1. 课程开始前3小时，给老师和学员发送即将上课提醒消息；
2. 课程开始前1小时，执行最少开课人数检查，如果少于最少订课人数，自动取消排课；
3. 课程开始后，给上课老师计算本课程的课时费用；
4. 课程结束后，给学员发送课后点评提示消息，点评本节课。
5. ...

# Usage

1. Add dependency to pom.xml.
    ```xml
    <dependency>
        <groupId>com.github.bingoohuang</groupId>
        <artifactId>sql-trigger</artifactId>
        <version>{sql-trigger-version}</version>
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
    