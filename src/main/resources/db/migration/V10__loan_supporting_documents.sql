-- Add supporting document URLs to loan applications
alter table loan_applications
    add column if not exists income_proof_url text,
    add column if not exists collateral_proof_url text;