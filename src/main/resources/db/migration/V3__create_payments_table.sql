-- V3: Create payments table
-- Payment entity with state machine for payment lifecycle

CREATE TABLE payments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id                UUID NOT NULL REFERENCES orders(id),
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    authorized_amount       BIGINT,           -- Amount authorized (smallest currency unit)
    captured_amount         BIGINT NOT NULL DEFAULT 0,  -- Amount captured so far
    refunded_amount         BIGINT NOT NULL DEFAULT 0,  -- Amount refunded so far
    currency_code           VARCHAR(3) NOT NULL,
    payment_method          VARCHAR(50),      -- CARD, UPI, WALLET, etc.
    payment_provider        VARCHAR(50),      -- STRIPE, RAZORPAY, etc.
    provider_payment_id     VARCHAR(255),     -- External payment ID from provider
    provider_auth_code      VARCHAR(255),     -- Authorization code from provider
    authorized_at           TIMESTAMP WITH TIME ZONE,
    captured_at             TIMESTAMP WITH TIME ZONE,
    expires_at              TIMESTAMP WITH TIME ZONE, -- Authorization expiry
    failure_reason          TEXT,
    metadata                JSONB,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version                 INTEGER NOT NULL DEFAULT 0,

    -- Constraints
    CONSTRAINT chk_payment_status CHECK (status IN (
        'PENDING', 'AUTHORIZED', 'AUTH_FAILED', 'CAPTURE_PENDING', 'CAPTURED',
        'CAPTURE_FAILED', 'REFUND_PENDING', 'PARTIALLY_REFUNDED', 'FULLY_REFUNDED', 'CANCELLED'
    )),
    CONSTRAINT chk_refund_not_exceed_capture CHECK (refunded_amount <= captured_amount),
    CONSTRAINT chk_captured_not_exceed_authorized CHECK (
        authorized_amount IS NULL OR captured_amount <= authorized_amount
    )
);

-- One payment per order (enforced by unique index)
CREATE UNIQUE INDEX idx_payments_order_id ON payments(order_id);

-- Indexes for queries
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_provider_payment_id ON payments(provider_payment_id);
CREATE INDEX idx_payments_captured_at ON payments(captured_at);
CREATE INDEX idx_payments_created_at ON payments(created_at);

-- Comments
COMMENT ON TABLE payments IS 'Payment state machine for order payment lifecycle';
COMMENT ON COLUMN payments.authorized_amount IS 'Amount authorized in smallest currency unit';
COMMENT ON COLUMN payments.captured_amount IS 'Amount captured so far (supports partial capture)';
COMMENT ON COLUMN payments.refunded_amount IS 'Total amount refunded (cumulative)';
COMMENT ON COLUMN payments.version IS 'Optimistic locking version';
