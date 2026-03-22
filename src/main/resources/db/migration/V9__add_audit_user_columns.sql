-- V9: Add audit user columns (created_by, last_modified_by)
-- These columns track who created and last modified each record

-- Add audit columns to orders table
ALTER TABLE orders ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(100);

-- Add audit columns to payments table
ALTER TABLE payments ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(100);

-- Add audit columns to reconciliation_reports table
ALTER TABLE reconciliation_reports ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE reconciliation_reports ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(100);

-- Add audit columns to restaurants table
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(100);

-- Set default value for existing records
UPDATE orders SET created_by = 'system', last_modified_by = 'system' WHERE created_by IS NULL;
UPDATE payments SET created_by = 'system', last_modified_by = 'system' WHERE created_by IS NULL;
UPDATE reconciliation_reports SET created_by = 'system', last_modified_by = 'system' WHERE created_by IS NULL;
UPDATE restaurants SET created_by = 'system', last_modified_by = 'system' WHERE created_by IS NULL;

-- Add indexes for audit columns (useful for filtering by user)
CREATE INDEX IF NOT EXISTS idx_orders_created_by ON orders(created_by);
CREATE INDEX IF NOT EXISTS idx_payments_created_by ON payments(created_by);
CREATE INDEX IF NOT EXISTS idx_reconciliation_reports_created_by ON reconciliation_reports(created_by);
