-- V4: Create payment_transactions table (Immutable Ledger)
-- Append-only transaction log for complete audit trail

CREATE TABLE payment_transactions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id          UUID NOT NULL REFERENCES payments(id),
    transaction_type    VARCHAR(30) NOT NULL,
    amount              BIGINT NOT NULL,
    currency_code       VARCHAR(3) NOT NULL,
    status              VARCHAR(30) NOT NULL,
    idempotency_key     VARCHAR(255) NOT NULL,
    provider_txn_id     VARCHAR(255),    -- External transaction reference from provider
    provider_response   JSONB,           -- Raw provider response for debugging
    error_code          VARCHAR(50),
    error_message       TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_txn_type CHECK (transaction_type IN (
        'AUTHORIZE', 'CAPTURE', 'REFUND', 'VOID'
    )),
    CONSTRAINT chk_txn_status CHECK (status IN (
        'INITIATED', 'SUCCESS', 'FAILED', 'TIMEOUT'
    )),
    CONSTRAINT chk_txn_positive_amount CHECK (amount > 0)
);

-- Critical: Prevent duplicate transactions with same idempotency key
-- This is the database-level idempotency guarantee
CREATE UNIQUE INDEX idx_payment_txn_idempotency
    ON payment_transactions(payment_id, transaction_type, idempotency_key);

-- Indexes for queries
CREATE INDEX idx_payment_txn_payment_id ON payment_transactions(payment_id);
CREATE INDEX idx_payment_txn_created_at ON payment_transactions(created_at);
CREATE INDEX idx_payment_txn_type_status ON payment_transactions(transaction_type, status);
CREATE INDEX idx_payment_txn_provider_txn_id ON payment_transactions(provider_txn_id);

-- Comments
COMMENT ON TABLE payment_transactions IS 'Immutable transaction ledger - append only';
COMMENT ON COLUMN payment_transactions.idempotency_key IS 'Client-provided key for idempotent operations';
COMMENT ON COLUMN payment_transactions.provider_response IS 'Raw JSON response from payment provider';
