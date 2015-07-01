-- DB setup
--
-- All the do$$ stuff are hacks because Postgres does not
-- have something like CREATE INDEX IF NOT EXISTS

create extension if not exists "pgcrypto";
create extension if not exists "uuid-ossp";

create table if not exists auth (
  auth_id uuid not null primary key default uuid_generate_v1mc(),
  username text not null unique,
  password text not null,
  created_at timestamp with time zone not null default current_timestamp,
  updated_at timestamp with time zone not null default current_timestamp
);

create table if not exists org (
  org_id uuid not null primary key default uuid_generate_v1mc(),
  name text not null unique,
  created_at timestamp with time zone not null default current_timestamp,
  updated_at timestamp with time zone not null default current_timestamp
);

create table if not exists org_membership (
  org_membershipt_id uuid not null primary key default uuid_generate_v1mc(),
  org_id uuid not null references org (org_id) on delete cascade on update restrict,
  auth_id uuid not null references auth (auth_id) on delete cascade on update restrict,
  created_at timestamp with time zone not null default current_timestamp,
  unique (org_id, auth_id)
);

create table if not exists dataset (
  dataset_id uuid not null primary key,
  ident int4 not null,
  name text not null,
  dataset_type char not null,
  schema json not null,
  org_id uuid not null references org (org_id) on delete cascade on update restrict,
  created_at timestamp with time zone not null default current_timestamp,
  updated_at timestamp with time zone not null default current_timestamp,
  unique (org_id, ident)
);

create table if not exists dataset_item (
  dataset_item_id uuid not null primary key,
  ident int4 not null,
  provided_id text,
  data json not null,
  item_type char not null,
  rvalue double precision,
  cvalue text,
  dataset_id uuid not null references dataset (dataset_id) on delete cascade on update restrict,
  created_at timestamp with time zone not null default current_timestamp,
  unique(ident, dataset_id),
  unique(provided_id, dataset_id)
);

create table if not exists experiment (
  experiment_id uuid not null primary key,
  ident int4 not null,
  name text not null,
  org_id uuid not null references org (org_id) on delete cascade on update restrict,
  created_at timestamp with time zone not null default current_timestamp,
  updated_at timestamp with time zone not null default current_timestamp,
  unique (ident, org_id),
  unique (name, org_id)
);

create table if not exists experiment_item (
  experiment_item_id uuid not null primary key,
  ident int4 not null,
  rvalue double precision,
  cvalue text,
  experiment_id uuid not null references experiment (experiment_id) on delete cascade on update restrict,
  dataset_item_id uuid not null references dataset_item (dataset_item_id) on delete cascade on update restrict,
  created_at timestamp with time zone not null default current_timestamp,
  unique(experiment_id, ident),
  unique(experiment_id, dataset_item_id)
);

create table if not exists comment (
  comment_id uuid not null primary key,
  ident int4 not null,
  subject_id uuid not null,
  user_name text not null,
  comment text not null,
  created_at timestamp with time zone not null default current_timestamp
);

create table if not exists labelling (
  labelling_id uuid not null primary key default uuid_generate_v1mc(),
  label text not null,
  object_id uuid not null,
  unique(label, object_id),
  created_at timestamp with time zone not null default current_timestamp
);
