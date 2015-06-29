-- DB setup
--
-- All the do$$ stuff are hacks because Postgres does not
-- have something like CREATE INDEX IF NOT EXISTS

create extension if not exists "uuid-ossp";

create table if not exists auth (
  auth_id uuid not null primary key default uuid_generate_v1mc(),
  auth_value text not null,
  metadata json,
  created_at timestamp with time zone not null default current_timestamp,
  updated_at timestamp with time zone not null default current_timestamp,
  unique (auth_value)
);

create table if not exists acl (
  acl_id uuid not null primary key default uuid_generate_v1mc(),
  auth_id uuid not null references auth (auth_id) on delete cascade on update restrict,
  other_id uuid not null,
  created_at timestamp with time zone not null default current_timestamp,
  unique (auth_id, other_id)
);

do $$
begin
if not exists (select 1 from pg_class where relname = 'acl_auth_idx') then
  create index acl_auth_idx on acl using btree (auth_id);
end if;
end$$;
