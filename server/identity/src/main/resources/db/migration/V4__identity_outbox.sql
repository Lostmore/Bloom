ALTER TABLE accounts ADD COLUMN event_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE outbox_events (
    sequence BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id UUID NOT NULL UNIQUE,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);
CREATE INDEX outbox_pending ON outbox_events (aggregate_id, sequence) WHERE published_at IS NULL;
CREATE INDEX outbox_retry ON outbox_events (next_attempt_at, sequence) WHERE published_at IS NULL;
CREATE INDEX outbox_published ON outbox_events (published_at) WHERE published_at IS NOT NULL;
