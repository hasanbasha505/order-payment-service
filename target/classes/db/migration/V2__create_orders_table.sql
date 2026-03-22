-- V2: Create orders table
-- Order entity representing customer purchases

CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number    VARCHAR(50) NOT NULL,
    restaurant_id   UUID NOT NULL REFERENCES restaurants(id),
    customer_id     UUID NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    total_amount    BIGINT NOT NULL,  -- Amount in smallest currency unit (cents/paise)
    currency_code   VARCHAR(3) NOT NULL DEFAULT 'USD',
    metadata        JSONB,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version         INTEGER NOT NULL DEFAULT 0,

    -- Constraints
    CONSTRAINT uk_orders_order_number UNIQUE(order_number),
    CONSTRAINT chk_order_status CHECK (status IN (
        'CREATED', 'PAYMENT_PENDING', 'PAYMENT_AUTHORIZED',
        'PAYMENT_CAPTURED', 'COMPLETED', 'CANCELLED', 'REFUNDED'
    )),
    CONSTRAINT chk_order_positive_amount CHECK (total_amount > 0)
);

-- Indexes for common queries
CREATE INDEX idx_orders_restaurant_id ON orders(restaurant_id);
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_restaurant_created ON orders(restaurant_id, created_at);

-- Comments
COMMENT ON TABLE orders IS 'Customer orders with payment status tracking';
COMMENT ON COLUMN orders.total_amount IS 'Amount in smallest currency unit (cents for USD)';
COMMENT ON COLUMN orders.version IS 'Optimistic locking version';
