-- V1: Create restaurants table
-- Restaurant entity with timezone information for reconciliation

CREATE TABLE restaurants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    timezone        VARCHAR(50) NOT NULL,  -- IANA timezone (e.g., 'Asia/Kolkata', 'America/New_York')
    currency_code   VARCHAR(3) NOT NULL DEFAULT 'USD',
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_restaurants_timezone ON restaurants(timezone);
CREATE INDEX idx_restaurants_active ON restaurants(active);

-- Comments
COMMENT ON TABLE restaurants IS 'Restaurant entity with timezone for reconciliation';
COMMENT ON COLUMN restaurants.timezone IS 'IANA timezone identifier (e.g., Asia/Kolkata)';
COMMENT ON COLUMN restaurants.currency_code IS 'ISO 4217 currency code';
