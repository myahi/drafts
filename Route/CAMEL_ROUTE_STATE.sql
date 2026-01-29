create table CAMEL_ROUTE_STATE (
  ROUTE_ID varchar2(255) primary key,
  DESIRED_STATE varchar2(16) not null,
  UPDATED_AT timestamp not null
);
