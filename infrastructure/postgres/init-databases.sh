#!/bin/sh
set -eu

: "${IDENTITY_DATABASE_PASSWORD:?Set the Identity database password in compose.yml}"
: "${USERS_DATABASE_PASSWORD:?Set the Users database password in compose.yml}"
: "${CHAT_DATABASE_PASSWORD:?Set the Chat database password in compose.yml}"

psql --username "$POSTGRES_USER" --dbname postgres --set=ON_ERROR_STOP=1 \
    --set=identity_password="$IDENTITY_DATABASE_PASSWORD" \
    --set=users_password="$USERS_DATABASE_PASSWORD" \
    --set=chat_password="$CHAT_DATABASE_PASSWORD" <<'SQL'
CREATE ROLE bloom_identity LOGIN PASSWORD :'identity_password';
CREATE ROLE bloom_users LOGIN PASSWORD :'users_password';
CREATE ROLE bloom_chat LOGIN PASSWORD :'chat_password';

CREATE DATABASE bloom_identity OWNER bloom_identity;
CREATE DATABASE bloom_users OWNER bloom_users;
CREATE DATABASE bloom_chat OWNER bloom_chat;

REVOKE ALL ON DATABASE bloom_identity FROM PUBLIC;
REVOKE ALL ON DATABASE bloom_users FROM PUBLIC;
REVOKE ALL ON DATABASE bloom_chat FROM PUBLIC;
SQL
