#!/bin/sh
set -eu

: "${PGPASSWORD:?Set the existing PostgreSQL administrator password}"
: "${INTERACTIONS_DATABASE_PASSWORD:?Set the Interactions database password}"

# Also works with an existing database volume; never drop or recreate user data.
psql --set=ON_ERROR_STOP=1 --set=password="$INTERACTIONS_DATABASE_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE bloom_interactions LOGIN PASSWORD %L', :'password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bloom_interactions')
\gexec
SELECT 'CREATE DATABASE bloom_interactions OWNER bloom_interactions'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'bloom_interactions')
\gexec
REVOKE ALL ON DATABASE bloom_interactions FROM PUBLIC;
SQL
