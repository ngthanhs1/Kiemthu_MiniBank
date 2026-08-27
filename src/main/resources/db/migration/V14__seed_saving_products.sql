-- Upsert saving products (idempotent)
with upserted as (
  insert into saving_products (
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
  values
    ('TK6T',  'Tiet kiem 6 thang',  'VND',
     'MONTH', 6,
     'fixed', 0.0520, null, null,
     'daily', 'end_of_term', true,
     1000000, null,
     0, 0, 0, 0, 0.0050, 0, null, null, null,
     'active', now(), now()),

    ('TK12T', 'Tiet kiem 12 thang', 'VND',
     'MONTH', 12,
     'fixed', 0.0580, null, null,
     'daily', 'end_of_term', true,
     5000000, null,
     0, 0, 0, 0, 0.0100, 0, null, null, null,
     'active', now(), now()),

    ('TK24T', 'Tiet kiem 24 thang', 'VND',
     'MONTH', 24,
     'fixed', 0.0650, null, null,
     'daily', 'end_of_term', true,
     10000000, null,
     0, 0, 0, 0, 0.0150, 0, null, null, null,
     'active', now(), now())
  on conflict (code) do update set
    name = excluded.name,
    currency = excluded.currency,
    term_unit = excluded.term_unit,
    term_value = excluded.term_value,
    interest_rate_type = excluded.interest_rate_type,
    base_interest_rate = excluded.base_interest_rate,
    penalty_interest_rate = excluded.penalty_interest_rate,
    bonus_interest_rate = excluded.bonus_interest_rate,
    interest_accrual_frequency = excluded.interest_accrual_frequency,
    interest_posting_frequency = excluded.interest_posting_frequency,
    capitalized = excluded.capitalized,
    min_open_amount = excluded.min_open_amount,
    max_open_amount = excluded.max_open_amount,
    deposit_fee_rate = excluded.deposit_fee_rate,
    deposit_fee_flat = excluded.deposit_fee_flat,
    withdrawal_fee_rate = excluded.withdrawal_fee_rate,
    withdrawal_fee_flat = excluded.withdrawal_fee_flat,
    close_fee_rate = excluded.close_fee_rate,
    close_fee_flat = excluded.close_fee_flat,
    management_fee_rate = excluded.management_fee_rate,
    management_fee_flat = excluded.management_fee_flat,
    management_fee_frequency = excluded.management_fee_frequency,
    status = excluded.status,
    updated_at = now()
  returning id, code
),
cleared as (
  delete from saving_product_interest_tiers t
  using upserted u
  where t.saving_product_id = u.id
)
insert into saving_product_interest_tiers (
  saving_product_id, min_amount, max_amount, interest_rate, effective_from, effective_to
)
select
  u.id,
  v.min_amount,
  v.max_amount,
  v.interest_rate,
  current_date,
  null
from upserted u
join (values
  ('TK6T',  1000000,   50000000, 0.0520),
  ('TK6T',  50000001,  200000000, 0.0550),
  ('TK6T',  200000001, null,     0.0580),

  ('TK12T',  5000000,   50000000, 0.0580),
  ('TK12T',  50000001,  200000000, 0.0610),
  ('TK12T',  200000001, null,     0.0640),

  ('TK24T',  10000000,   50000000, 0.0650),
  ('TK24T',  50000001,  200000000, 0.0680),
  ('TK24T',  200000001, null,     0.0710)
) as v(code, min_amount, max_amount, interest_rate)
on u.code = v.code;