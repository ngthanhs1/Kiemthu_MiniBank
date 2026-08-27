-- Expand mortgage loan product limits for higher secured loans
update loan_products
set max_amount = 3000000000,
    updated_at = now()
where code = 'VAY_THE_CHAP';