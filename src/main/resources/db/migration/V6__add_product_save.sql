-- === Upsert saving products ===
WITH upserted AS (
  INSERT INTO saving_products (
    code, name, currency,
    term_unit, term_value,
    interest_rate_type, base_interest_rate, penalty_interest_rate, bonus_interest_rate,
    interest_accrual_frequency, interest_posting_frequency, capitalized,
    min_open_amount, max_open_amount,
    deposit_fee_rate, deposit_fee_flat,
    withdrawal_fee_rate, withdrawal_fee_flat,
    close_fee_rate, close_fee_flat,
    management_fee_rate, management_fee_flat, management_fee_frequency,
    status, created_at, updated_at
  )
  VALUES
    ('TK6T',  'Tiet kiem 6 thang',  'VND',
     'MONTH', 6,
     'fixed', 0.0520, NULL, NULL,
     'daily', 'end_of_term', true,
     1000000, NULL,
     0, 0, 0, 0, 0.0050, 0, NULL, NULL, NULL,
     'active', now(), now()),

    ('TK12T', 'Tiet kiem 12 thang', 'VND',
     'MONTH', 12,
     'fixed', 0.0580, NULL, NULL,
     'daily', 'end_of_term', true,
     5000000, NULL,
     0, 0, 0, 0, 0.0100, 0, NULL, NULL, NULL,
     'active', now(), now()),

    ('TK24T', 'Tiet kiem 24 thang', 'VND',
     'MONTH', 24,
     'fixed', 0.0650, NULL, NULL,
     'daily', 'end_of_term', true,
     10000000, NULL,
     0, 0, 0, 0, 0.0150, 0, NULL, NULL, NULL,
     'active', now(), now())
  ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    currency = EXCLUDED.currency,
    term_unit = EXCLUDED.term_unit,
    term_value = EXCLUDED.term_value,
    interest_rate_type = EXCLUDED.interest_rate_type,
    base_interest_rate = EXCLUDED.base_interest_rate,
    penalty_interest_rate = EXCLUDED.penalty_interest_rate,
    bonus_interest_rate = EXCLUDED.bonus_interest_rate,
    interest_accrual_frequency = EXCLUDED.interest_accrual_frequency,
    interest_posting_frequency = EXCLUDED.interest_posting_frequency,
    capitalized = EXCLUDED.capitalized,
    min_open_amount = EXCLUDED.min_open_amount,
    max_open_amount = EXCLUDED.max_open_amount,
    deposit_fee_rate = EXCLUDED.deposit_fee_rate,
    deposit_fee_flat = EXCLUDED.deposit_fee_flat,
    withdrawal_fee_rate = EXCLUDED.withdrawal_fee_rate,
    withdrawal_fee_flat = EXCLUDED.withdrawal_fee_flat,
    close_fee_rate = EXCLUDED.close_fee_rate,
    close_fee_flat = EXCLUDED.close_fee_flat,
    management_fee_rate = EXCLUDED.management_fee_rate,
    management_fee_flat = EXCLUDED.management_fee_flat,
    management_fee_frequency = EXCLUDED.management_fee_frequency,
    status = EXCLUDED.status,
    updated_at = now()
  RETURNING id, code
),
cleared AS (
  DELETE FROM saving_product_interest_tiers t
  USING upserted u
  WHERE t.saving_product_id = u.id
)
INSERT INTO saving_product_interest_tiers (
  saving_product_id, min_amount, max_amount, interest_rate, effective_from, effective_to
)
SELECT
  u.id,
  v.min_amount,
  v.max_amount,
  v.interest_rate,
  CURRENT_DATE,
  NULL
FROM upserted u
JOIN (VALUES
  -- TK6T
  ('TK6T',  1000000,   50000000, 0.0520),
  ('TK6T',  50000001,  200000000, 0.0550),
  ('TK6T',  200000001, NULL,     0.0580),

  -- TK12T
  ('TK12T',  5000000,   50000000, 0.0580),
  ('TK12T',  50000001,  200000000, 0.0610),
  ('TK12T',  200000001, NULL,     0.0640),

  -- TK24T
  ('TK24T',  10000000,   50000000, 0.0650),
  ('TK24T',  50000001,  200000000, 0.0680),
  ('TK24T',  200000001, NULL,     0.0710)
) AS v(code, min_amount, max_amount, interest_rate)
ON u.code = v.code;