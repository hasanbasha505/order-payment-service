-- Add version columns for optimistic locking
-- Required for @Version fields added to IdempotencyKey and Restaurant entities

ALTER TABLE idempotency_keys ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE restaurants ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
