-- Remove seeded test account so users can choose their own account number
DELETE FROM accounts
WHERE account_number = '1000001'
  AND account_name LIKE 'Test Account%'
  AND user_id IN (SELECT id FROM users WHERE phone = '0981712585');
