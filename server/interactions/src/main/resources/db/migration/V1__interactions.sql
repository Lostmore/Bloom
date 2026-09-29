CREATE TABLE account_guards (
    user_id UUID PRIMARY KEY,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE interaction_pairs (
    user_a UUID NOT NULL,
    user_b UUID NOT NULL,
    reaction_a VARCHAR(20),
    reaction_b VARCHAR(20),
    match_id UUID,
    cooldown_until TIMESTAMPTZ,
    PRIMARY KEY (user_a, user_b),
    CHECK (user_a < user_b),
    CHECK (reaction_a IN ('LIKE', 'SKIP', 'SUPER_INTEREST')),
    CHECK (reaction_b IN ('LIKE', 'SKIP', 'SUPER_INTEREST'))
);
CREATE INDEX pairs_by_b ON interaction_pairs (user_b, user_a);

CREATE TABLE matches (
    id UUID PRIMARY KEY,
    user_a UUID NOT NULL,
    user_b UUID NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'UNMATCHED', 'BLOCKED')),
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (user_a < user_b)
);
CREATE UNIQUE INDEX one_active_match_per_pair ON matches (user_a, user_b) WHERE status = 'ACTIVE';
CREATE INDEX matches_by_a ON matches (user_a, id) WHERE status = 'ACTIVE';
CREATE INDEX matches_by_b ON matches (user_b, id) WHERE status = 'ACTIVE';

CREATE TABLE interaction_requests (
    actor_id UUID NOT NULL,
    request_id UUID NOT NULL,
    target_id UUID NOT NULL,
    reaction VARCHAR(20) NOT NULL,
    response JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (actor_id, request_id)
);
CREATE INDEX requests_daily_count ON interaction_requests (actor_id, created_at);

CREATE TABLE outbox_events (
    sequence BIGSERIAL PRIMARY KEY,
    id UUID NOT NULL UNIQUE,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX outbox_pending ON outbox_events (aggregate_id, sequence) WHERE published_at IS NULL;
CREATE TABLE consumed_events (
    event_id UUID PRIMARY KEY,
    consumed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
