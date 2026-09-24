CREATE TABLE security_audit (
    id UUID PRIMARY KEY,
    account_id UUID,
    action VARCHAR(40) NOT NULL,
    reference_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX security_audit_account ON security_audit (account_id, created_at);

CREATE TABLE auth_attempts (
    key_hash VARCHAR(64) PRIMARY KEY,
    attempts INTEGER NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    next_attempt_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX refresh_token_hash_unique ON refresh_sessions (token_hash);
CREATE INDEX refresh_active_family ON refresh_sessions (account_id, family_id, expires_at)
    WHERE revoked = false;

ALTER TABLE accounts ADD CONSTRAINT accounts_status_valid
    CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'));
