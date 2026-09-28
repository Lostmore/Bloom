CREATE TABLE profiles (
    id UUID PRIMARY KEY,
    nickname VARCHAR(40) NOT NULL,
    birth_date DATE,
    gender VARCHAR(20),
    bio VARCHAR(1000) NOT NULL DEFAULT '',
    city VARCHAR(100) NOT NULL DEFAULT '',
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    relationship_goal VARCHAR(80) NOT NULL DEFAULT '',
    search_modes JSONB NOT NULL DEFAULT '[]',
    preferences JSONB NOT NULL,
    privacy JSONB NOT NULL,
    interests JSONB NOT NULL DEFAULT '[]',
    photos JSONB NOT NULL DEFAULT '[]',
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_seen TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CHECK (deleted OR (length(trim(nickname)) > 0 AND birth_date IS NOT NULL AND gender IS NOT NULL)),
    CHECK (gender IN ('MALE', 'FEMALE', 'NON_BINARY', 'OTHER')),
    CHECK (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180),
    CHECK ((latitude IS NULL) = (longitude IS NULL)),
    CHECK (jsonb_typeof(search_modes) = 'array' AND jsonb_array_length(search_modes) <= 4),
    CHECK (jsonb_typeof(interests) = 'array' AND jsonb_array_length(interests) <= 15),
    CHECK (jsonb_typeof(photos) = 'array' AND jsonb_array_length(photos) <= 6)
);

CREATE TABLE interests (
    id VARCHAR(40) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    icon VARCHAR(40) NOT NULL,
    category VARCHAR(40) NOT NULL
);

INSERT INTO interests (id, name, icon, category) VALUES
    ('music', 'Music', 'music', 'culture'),
    ('movies', 'Movies', 'film', 'culture'),
    ('gaming', 'Gaming', 'gamepad', 'leisure'),
    ('travel', 'Travel', 'plane', 'leisure'),
    ('coffee', 'Coffee', 'coffee', 'food'),
    ('food', 'Food', 'utensils', 'food'),
    ('sport', 'Sport', 'ball', 'active'),
    ('art', 'Art', 'palette', 'culture'),
    ('photography', 'Photography', 'camera', 'culture'),
    ('technology', 'Technology', 'cpu', 'learning'),
    ('books', 'Books', 'book', 'culture'),
    ('nightlife', 'Nightlife', 'moon', 'leisure'),
    ('nature', 'Nature', 'leaf', 'outdoors'),
    ('animals', 'Animals', 'paw', 'outdoors'),
    ('fitness', 'Fitness', 'dumbbell', 'active'),
    ('concerts', 'Concerts', 'mic', 'culture'),
    ('walking', 'Walking', 'footprints', 'active'),
    ('cooking', 'Cooking', 'chef', 'food');

CREATE TABLE user_blocks (
    owner_id UUID NOT NULL REFERENCES profiles(id),
    target_id UUID NOT NULL REFERENCES profiles(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (owner_id, target_id),
    CHECK (owner_id <> target_id)
);
CREATE INDEX user_blocks_target ON user_blocks (target_id, owner_id);

CREATE TABLE reports (
    id UUID PRIMARY KEY,
    reporter_id UUID NOT NULL REFERENCES profiles(id),
    target_id UUID NOT NULL REFERENCES profiles(id),
    reason VARCHAR(32) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (reporter_id <> target_id),
    CHECK (reason IN ('FAKE_PROFILE', 'HARASSMENT', 'SPAM', 'INAPPROPRIATE_CONTENT', 'UNDERAGE', 'OTHER'))
);
CREATE INDEX reports_reporter_created ON reports (reporter_id, created_at);

CREATE TABLE user_audit (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id UUID NOT NULL,
    action VARCHAR(40) NOT NULL,
    target_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX user_audit_created ON user_audit (created_at);

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
