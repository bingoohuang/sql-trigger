drop table  if exists t_schedule;
create table t_schedule (
  id varchar(20) primary key,
  name varchar(20) not null,
  schedule_state varchar(3) not null comment '状态',
  subscribes int not null default 0 comment '订课人数'
  );

insert into t_schedule(id, name, schedule_state, subscribes)
values ('1', 'bingoo', 'OK', 0)
,('2', 'dingoo', 'OK', 7);

select id, name, schedule_state from T_SCHEDULE;