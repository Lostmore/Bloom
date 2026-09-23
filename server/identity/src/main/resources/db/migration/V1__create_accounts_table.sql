CREATE TABLE accounts (
    id              UUID            PRIMARY KEY,
    phone_number    VARCHAR(20)     NOT NULL,
    password_hash   VARCHAR(512)    NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    token_version   BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP       NOT NULL,
    last_seen_at    TIMESTAMP
);

CREATE UNIQUE INDEX idx_accounts_phone ON accounts (phone_number);
CREATE INDEX idx_accounts_status ON accounts (status);
