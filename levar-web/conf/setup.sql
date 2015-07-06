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
  provided_id text not null unique,
  name text,
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
  provided_id text not null,
  name text,
  dataset_type char not null,
  schema json not null,
  org_id uuid not null references org (org_id) on delete cascade on update restrict,
  created_at timestamp with time zone not null default current_timestamp,
  updated_at timestamp with time zone not null default current_timestamp,
  unique (org_id, ident)
);

create table if not exists datum (
  datum_id uuid not null primary key,
  ident int4 not null,
  provided_id text,
  data json not null,
  rvalue double precision,
  cvalue text,
  dataset_id uuid not null references dataset (dataset_id) on delete cascade on update restrict,
  created_at timestamp with time zone not null default current_timestamp,
  unique(ident, dataset_id),
  unique(provided_id, dataset_id)
);

create table if not exists experiment (
  experiment_id uuid not null primary key default uuid_generate_v1mc(),
  provided_id text not null,
  name text,
  org_id uuid not null references org (org_id) on delete cascade on update restrict,
  created_at timestamp with time zone not null default current_timestamp,
  updated_at timestamp with time zone not null default current_timestamp,
  unique (provided_id, org_id)
);

create table if not exists experiment_for_dataset (
  experiment_for_dataset_id uuid not null primary key,
  experiment_id uuid not null references experiment (experiment_id) on delete cascade on update restrict,
  dataset_id uuid not null references dataset (dataset_id) on delete cascade on update restrict,
  created_at timestamp with time zone not null default current_timestamp,
  unique(experiment_id, dataset_id)
);

create table if not exists prediction (
  prediction_id uuid not null primary key,
  ident int4 not null,
  rvalue double precision,
  cvalue text,
  predict_score double precision,
  experiment_id uuid not null references experiment (experiment_id) on delete cascade on update restrict,
  datum_id uuid not null references datum (datum_id) on delete cascade on update restrict,
  created_at timestamp with time zone not null default current_timestamp,
  unique(experiment_id, ident),
  unique(experiment_id, datum_id)
);

create table if not exists comment (
  comment_id uuid not null primary key,
  ident int4 not null,
  subject_id uuid not null,
  username text not null,
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

create table if not exists job (
  job_id uuid not null primary key default uuid_generate_v1mc(),
  name text not null,
  org_id uuid not null references org (org_id) on delete cascade on update restrict,
  api_url text not null,
  api_auth text,
  request_type char not null,
  request_body_field text,
  response_path text not null,
  run_period char not null,
  created_at timestamp with time zone not null default current_timestamp,
  updated_at timestamp with time zone not null default current_timestamp,
  unique(org_id, name)
);

create table if not exists job_dataset (
  job_dataset_id uuid not null primary key default uuid_generate_v1mc(),
  job_id uuid not null references job (job_id) on delete cascade on update restrict,
  dataset_id uuid not null references dataset (dataset_id) on delete cascade on update restrict,
  created_at timestamp with time zone not null default current_timestamp,
  unique (job_id, dataset_id)
);

create table if not exists run (
  run_id uuid not null primary key default uuid_generate_v1mc(),
  job_id uuid not null references job (job_id) on delete cascade on update restrict,
  adhoc boolean not null default false,
  created_at timestamp with time zone not null default current_timestamp
);

create table if not exists call (
  call_id uuid not null primary key,
  prediction_id uuid not null references prediction (prediction_id) on delete cascade on update restrict,
  run_id uuid not null references run (run_id) on delete cascade on update restrict,
  response_status int not null,
  response_time_milliseconds int not null,
  response_body_raw text,
  unique(prediction_id, run_id)
);
