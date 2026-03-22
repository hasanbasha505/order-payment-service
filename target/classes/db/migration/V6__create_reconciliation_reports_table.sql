-- V6: Create reconciliation_reports table
-- Daily settlement reports per restaurant with provider comparison

CREATE TABLE reconciliation_reports (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id           UUID NOT NULL REFERENCES restaurants(id),
    report_date             DATE NOT NULL,  -- In restaurant's local timezone

    -- Internal totals (from our database)
    total_orders            INTEGER NOT NULL DEFAULT 0,
    total_captured_amount   BIGINT NOT NULL DEFAULT 0,
    total_refunded_amount   BIGINT NOT NULL DEFAULT 0,
    net_amount              BIGINT NOT NULL DEFAULT 0,

    -- Provider totals (from payment provider)
    provider_total_captured BIGINT,
    provider_total_refunded BIGINT,
    provider_net_amount     BIGINT,

    -- Reconciliation status
    currency_code           VARCHAR(3) NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    match_status            VARCHAR(20) DEFAULT 'PENDING',

    -- Detailed data
    report_data             JSONB,  -- Detailed transaction breakdown
    discrepancies           JSONB,  -- List of mismatches if any

    -- Audit
    generated_at            TIMESTAMP WITH TIME ZONE,
    investigated_at         TIMESTAMP WITH TIME ZONE,
    investigated_by         VARCHAR(100),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uk_reconciliation_restaurant_date UNIQUE(restaurant_id, report_date),
    CONSTRAINT chk_report_status CHECK (status IN (
        'PENDING', 'GENERATED', 'VERIFIED', 'DISPUTED'
    )),
    CONSTRAINT chk_match_status CHECK (match_status IN (
        'PENDING', 'MATCHED', 'MISMATCH', 'INVESTIGATING', 'RESOLVED'
    ))
);

-- Indexes
CREATE INDEX idx_reconciliation_restaurant_id ON reconciliation_reports(restaurant_id);
CREATE INDEX idx_reconciliation_report_date ON reconciliation_reports(report_date);
CREATE INDEX idx_reconciliation_status ON reconciliation_reports(status);
CREATE INDEX idx_reconciliation_match_status ON reconciliation_reports(match_status);

-- Comments
COMMENT ON TABLE reconciliation_reports IS 'Daily settlement reports with provider comparison';
COMMENT ON COLUMN reconciliation_reports.report_date IS 'Date in restaurant local timezone';
COMMENT ON COLUMN reconciliation_reports.match_status IS 'Result of comparing internal vs provider data';
COMMENT ON COLUMN reconciliation_reports.discrepancies IS 'JSON array of mismatched transactions';
