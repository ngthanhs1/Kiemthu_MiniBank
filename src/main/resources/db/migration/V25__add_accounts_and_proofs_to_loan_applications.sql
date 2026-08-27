alter table loan_applications
    add column if not exists income_proof_url text,
    add column if not exists collateral_proof_url text,
    add column if not exists disbursement_account_id bigint,
    add column if not exists repayment_account_id bigint;

do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conname = 'fk_loan_applications_disbursement_account'
    ) then
        alter table loan_applications
            add constraint fk_loan_applications_disbursement_account
            foreign key (disbursement_account_id) references accounts(id);
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'fk_loan_applications_repayment_account'
    ) then
        alter table loan_applications
            add constraint fk_loan_applications_repayment_account
            foreign key (repayment_account_id) references accounts(id);
    end if;
end $$;

create index if not exists idx_loan_applications_disbursement_account_id
    on loan_applications(disbursement_account_id);

create index if not exists idx_loan_applications_repayment_account_id
    on loan_applications(repayment_account_id);