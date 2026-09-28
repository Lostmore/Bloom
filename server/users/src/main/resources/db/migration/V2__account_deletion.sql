-- Identity revocation has its own durable queue, independent of Kafka availability and event backlog.
CREATE TABLE account_deletions (
    user_id UUID PRIMARY KEY REFERENCES profiles(id),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX account_deletions_pending ON account_deletions (next_attempt_at) WHERE completed_at IS NULL;
