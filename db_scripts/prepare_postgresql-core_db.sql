CREATE USER "postgresql-core-superuser" WITH
  SUPERUSER
  ENCRYPTED PASSWORD 'superuser123';

CREATE USER "postgresql-core-owner" WITH
  NOSUPERUSER
  ENCRYPTED PASSWORD 'owner123';

CREATE USER "postgresql-core-user" WITH
  NOSUPERUSER
  ENCRYPTED PASSWORD 'user123';

CREATE DATABASE postgresql_core WITH
  OWNER = "postgresql-core-owner"
  ENCODING = 'UTF8'
  CONNECTION LIMIT = -1;

GRANT ALL PRIVILEGES ON DATABASE postgresql_core to "postgresql-core-owner" ;

\c postgresql_core;

CREATE TABLE public.users
(
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.users
    OWNER to "postgresql-core-owner";