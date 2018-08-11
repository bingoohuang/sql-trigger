drop table  if exists t_schedule;
create table t_schedule (id varchar(20) primary key,
  name varchar(20) not null,
  schedule_state varchar(3) not null comment '状态'
  );

