CREATE TABLE refresh_sessions (
    id              UUID            PRIMARY KEY,
    account_id      UUID            NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    token_hash      VARCHAR(512)    NOT NULL,
    family_id       UUID            NOT NULL,
    device_info     VARCHAR(255),
    ip_address      VARCHAR(45),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ     NOT NULL,
    revoked         BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_refresh_sessions_account ON refresh_sessions (account_id);
CREATE INDEX idx_refresh_sessions_family ON refresh_sessions (family_id);
CREATE INDEX idx_refresh_sessions_token_hash ON refresh_sessions (token_hash);
