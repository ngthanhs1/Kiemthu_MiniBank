-- Seed loan products for mobile applications
insert into loan_products (
    code, name, loan_type, currency,
    min_amount, max_amount, min_term_months, max_term_months,
    interest_rate_type, base_interest_rate, penalty_interest_rate, grace_interest_rate,
    processing_fee_rate, processing_fee_flat,
    early_repayment_fee_rate, early_repayment_fee_flat,
    interest_calculation_method, repayment_frequency,
    status, created_at, updated_at
)
values
    (
        'VAY_TIN_CHAP', 'Vay tin chap ca nhan', 'PERSONAL', 'VND',
        30000000, 1000000000, 6, 60,
        'FIXED', 0.0950, null, null,
        0, 0, 0, 0,
        'REDUCING_BALANCE', 'MONTHLY',
        'active', now(), now()
    ),
    (
        'VAY_THE_CHAP', 'Vay the chap', 'MORTGAGE', 'VND',
        50000000, 2000000000, 6, 120,
        'FIXED', 0.0850, null, null,
        0, 0, 0, 0,
        'REDUCING_BALANCE', 'MONTHLY',
        'active', now(), now()
    )
on conflict (code) do update set
    name = excluded.name,
    loan_type = excluded.loan_type,
    currency = excluded.currency,
    min_amount = excluded.min_amount,
    max_amount = excluded.max_amount,
    min_term_months = excluded.min_term_months,
    max_term_months = excluded.max_term_months,
    interest_rate_type = excluded.interest_rate_type,
    base_interest_rate = excluded.base_interest_rate,
    penalty_interest_rate = excluded.penalty_interest_rate,
    grace_interest_rate = excluded.grace_interest_rate,
    processing_fee_rate = excluded.processing_fee_rate,
    processing_fee_flat = excluded.processing_fee_flat,
    early_repayment_fee_rate = excluded.early_repayment_fee_rate,
    early_repayment_fee_flat = excluded.early_repayment_fee_flat,
    interest_calculation_method = excluded.interest_calculation_method,
    repayment_frequency = excluded.repayment_frequency,
    status = excluded.status,
    updated_at = now();