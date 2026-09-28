#!/bin/sh
set -eu

: "${BLOOM_DATABASE_PASSWORD:?Set the application database password in compose.yml}"
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set=app_password="$BLOOM_DATABASE_PASSWORD" <<'SQL'
CREATE ROLE bloom_users LOGIN PASSWORD :'app_password';
GRANT CONNECT ON DATABASE bloom_users TO bloom_users;
ALTER SCHEMA public OWNER TO bloom_users;
SQL
