-- Test user for development
-- Password: 123456 (BCrypt hash)
INSERT INTO users (phone, email, password_hash, full_name, status, device_id) VALUES
('0981712585', 'test@minibank.com', '$2a$10$CzFTpjOsD3K9.s5EI5K3X.KJ3aEZsL5c.vHz8hZBG3eVbDXYqPCxK', 'Test User', 'active', 'test-device-001')
ON CONFLICT (phone) DO NOTHING;

-- Insert test accounts for the user
INSERT INTO accounts (user_id, account_number, account_name, account_type, currency, available_balance, current_balance, status)
SELECT id, '1000001', 'Test Account 1', 'payment', 'VND', 10000000, 10000000, 'active'
FROM users WHERE phone = '0981712585'
ON CONFLICT (account_number) DO NOTHING;
