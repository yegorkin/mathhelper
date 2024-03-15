create sequence equations_seq;
create sequence roots_seq;

create table equations (
  id bigint primary key default nextval('equations_seq'),
  expression varchar(200) not null check (expression <> '')
);

create table roots (
  id bigint primary key default nextval('roots_seq'),
  eq_id bigint references equations (id) not null,
  value float8 not null,
  unique (eq_id, value)
);

create index roots_value_idx on roots(value);
