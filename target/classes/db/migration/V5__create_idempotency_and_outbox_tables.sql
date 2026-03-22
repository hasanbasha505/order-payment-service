-- V5: Create idempotency_keys and outbox_events tables
-- Request deduplication and transactional outbox pattern

-- Idempotency keys for HTTP-level request deduplication
CREATE TABLE idempotency_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key             VARCHAR(255) NOT NULL,
    request_hash    VARCHAR(64) NOT NULL,  -- SHA-256 of request body
    response_code   INTEGER,
    response_body   TEXT,  -- Cached response body
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_idempotency_key UNIQUE(key)
);

-- Index for cleanup job
CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);

-- Transactional Outbox for reliable event publishing
CREATE TABLE outbox_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type      VARCHAR(50) NOT NULL,   -- 'PAYMENT', 'ORDER'
    aggregate_id        UUID NOT NULL,          -- payment_id or order_id
    event_type          VARCHAR(50) NOT NULL,   -- 'PAYMENT_CAPTURED', 'REFUND_PROCESSED'
    payload             JSONB NOT NULL,         -- Full event data
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at        TIMESTAMP WITH TIME ZONE,
    retry_count         INTEGER NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);

-- Index for outbox publisher to find pending events efficiently
CREATE INDEX idx_outbox_pending ON outbox_events(status, created_at) WHERE status = 'PENDING';
CREATE INDEX idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);

-- Processed events table for consumer-side idempotency
CREATE TABLE processed_events (
    event_id        VARCHAR(50) PRIMARY KEY,
    processed_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for cleanup
CREATE INDEX idx_processed_events_date ON processed_events(processed_at);

-- Comments
COMMENT ON TABLE idempotency_keys IS 'HTTP-level request deduplication with cached responses';
COMMENT ON TABLE outbox_events IS 'Transactional outbox for reliable event publishing';
COMMENT ON TABLE processed_events IS 'Consumer-side event deduplication';
