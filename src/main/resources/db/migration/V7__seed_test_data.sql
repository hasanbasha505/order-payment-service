-- V7: Seed test data
-- Sample restaurants for development and testing

INSERT INTO restaurants (id, name, timezone, currency_code, active) VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'Joe''s Burgers - NYC', 'America/New_York', 'USD', true),
    ('550e8400-e29b-41d4-a716-446655440002', 'Pizza Palace - LA', 'America/Los_Angeles', 'USD', true),
    ('550e8400-e29b-41d4-a716-446655440003', 'Curry House - Mumbai', 'Asia/Kolkata', 'INR', true),
    ('550e8400-e29b-41d4-a716-446655440004', 'Sushi Express - Tokyo', 'Asia/Tokyo', 'JPY', true),
    ('550e8400-e29b-41d4-a716-446655440005', 'Fish & Chips - London', 'Europe/London', 'GBP', true),
    ('550e8400-e29b-41d4-a716-446655440006', 'Cafe Berlin', 'Europe/Berlin', 'EUR', true)
ON CONFLICT DO NOTHING;
