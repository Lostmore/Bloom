CREATE TABLE account_guards (
    user_id UUID PRIMARY KEY,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE activities (
    sequence BIGSERIAL UNIQUE NOT NULL,
    id UUID PRIMARY KEY,
    creator_id UUID NOT NULL,
    request_id UUID NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    category VARCHAR(20) NOT NULL CHECK (category IN
        ('COFFEE','WALK','CINEMA','DINNER','SPORT','CONCERT','EXHIBITION','GAMES','OTHER')),
    approximate_area VARCHAR(200) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    max_participants INTEGER NOT NULL CHECK (max_participants BETWEEN 2 AND 100),
    participant_count INTEGER NOT NULL DEFAULT 1 CHECK (participant_count >= 0 AND participant_count <= max_participants),
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','FULL','STARTED','FINISHED','CANCELLED')),
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (creator_id, request_id),
    CHECK (ends_at > starts_at)
);
CREATE INDEX activities_nearby ON activities (starts_at, latitude, sequence DESC) WHERE status IN ('OPEN','FULL');
CREATE INDEX activities_category ON activities (category, starts_at) WHERE status IN ('OPEN','FULL');
CREATE INDEX activities_creator ON activities (creator_id, sequence DESC);
CREATE INDEX activities_finish ON activities (ends_at) WHERE status = 'STARTED';

CREATE TABLE activity_participants (
    activity_id UUID NOT NULL REFERENCES activities(id),
    user_id UUID NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (activity_id, user_id)
);
CREATE INDEX participants_user ON activity_participants (user_id, activity_id);

CREATE TABLE nearby_limits (
    user_id UUID PRIMARY KEY,
    next_request_at TIMESTAMPTZ NOT NULL
);

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
