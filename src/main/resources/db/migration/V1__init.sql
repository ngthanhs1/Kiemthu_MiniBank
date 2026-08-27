-- Flyway migration: initial schema for MiniBank (PostgreSQL)
-- Note: uses timestamptz for Instant-friendly timestamps.

-- 1) Admin & RBAC
create table admin_users (
    id              bigserial primary key,
    username        varchar(100) not null unique,
    email           varchar(255) not null unique,
    password_hash   varchar(255) not null,
    full_name       varchar(255) not null,
    status          varchar(32) not null default 'active',
    created_at      timestamptz not null default now()
);

create table roles (
    id              bigserial primary key,
    code            varchar(50) not null unique,
    name            varchar(100) not null,
    description     text
);

create table admin_user_roles (
    id              bigserial primary key,
    admin_user_id   bigint not null references admin_users(id),
    role_id         bigint not null references roles(id),
    assigned_at     timestamptz not null default now(),
    unique (admin_user_id, role_id)
);

create index idx_admin_user_roles_admin_user_id on admin_user_roles(admin_user_id);
create index idx_admin_user_roles_role_id on admin_user_roles(role_id);

-- 2) Users & KYC
create table users (
    id                      bigserial primary key,
    phone                   varchar(20) not null unique,
    email                   varchar(255) not null unique,
    password_hash           varchar(255) not null,
    full_name               varchar(255),
    dob                     date,
    citizen_id              varchar(50),
    address                 text,
    status                  varchar(32) not null default 'pending',
    customer_rank           varchar(32) not null default 'dong',
    credit_score_level      varchar(16),
    transaction_pin_hash    varchar(255),
    public_key              text,
    device_id               varchar(255),
    created_at              timestamptz not null default now(),
    updated_at              timestamptz not null default now()
);

create index idx_users_status on users(status);

create table kyc_requests (
    id              bigserial primary key,
    user_id         bigint not null references users(id),
    full_name       varchar(255) not null,
    dob             date not null,
    citizen_id      varchar(50) not null,
    address         text not null,
    status          varchar(32) not null default 'pending',
    submitted_at    timestamptz not null default now(),
    reviewed_at     timestamptz,
    reviewed_by_id  bigint references admin_users(id),
    review_note     text
);

create index idx_kyc_requests_user_id on kyc_requests(user_id);
create index idx_kyc_requests_status on kyc_requests(status);
create index idx_kyc_requests_reviewed_by_id on kyc_requests(reviewed_by_id);

create table documents (
    id                  bigserial primary key,
    owner_type          varchar(50) not null,
    owner_id            bigint not null,
    document_type       varchar(100) not null,
    file_name           varchar(255),
    file_url            text not null,
    mime_type           varchar(100),
    verified_status     varchar(32) not null default 'pending',
    uploaded_by_type    varchar(32) not null,
    uploaded_by_id      bigint,
    uploaded_at         timestamptz not null default now(),
    verified_by_id      bigint references admin_users(id),
    verified_at         timestamptz,
    note                text
);

create index idx_documents_owner on documents(owner_type, owner_id);
create index idx_documents_verified_status on documents(verified_status);
create index idx_documents_verified_by_id on documents(verified_by_id);

create table user_sessions (
    id                  bigserial primary key,
    user_id             bigint not null references users(id),
    device_id           varchar(255) not null,
    refresh_token_hash  varchar(255) not null,
    is_active           boolean not null default true,
    last_login_at       timestamptz not null default now(),
    expires_at          timestamptz not null
);

create index idx_user_sessions_user_id on user_sessions(user_id);
create index idx_user_sessions_device_id on user_sessions(device_id);
create index idx_user_sessions_is_active on user_sessions(is_active);

-- 3) Accounts
create table accounts (
    id                      bigserial primary key,
    user_id                 bigint not null references users(id),
    account_number          varchar(32) not null unique,
    account_name            varchar(255) not null,
    account_type            varchar(32) not null default 'payment',
    currency                varchar(10) not null default 'VND',
    available_balance       numeric(18,2) not null default 0,
    current_balance         numeric(18,2) not null default 0,
    daily_transfer_limit    numeric(18,2) not null default 0,
    daily_receive_limit     numeric(18,2) not null default 0,
    status                  varchar(32) not null default 'active',
    opened_at               timestamptz not null default now(),
    closed_at               timestamptz
);

create index idx_accounts_user_id on accounts(user_id);
create index idx_accounts_status on accounts(status);

create table account_qr_codes (
    id              bigserial primary key,
    account_id      bigint not null references accounts(id),
    qr_payload      text not null,
    qr_image_url    text,
    created_at      timestamptz not null default now(),
    is_active       boolean not null default true
);

create index idx_account_qr_codes_account_id on account_qr_codes(account_id);
create index idx_account_qr_codes_is_active on account_qr_codes(is_active);

-- 4) Transactions
create table transactions (
    id                      bigserial primary key,
    transaction_code        varchar(64) not null unique,
    idempotency_key         varchar(128) unique,
    from_account_id         bigint references accounts(id),
    to_account_id           bigint references accounts(id),
    transaction_type        varchar(50) not null,
    amount                  numeric(18,2) not null,
    fee_amount              numeric(18,2) not null default 0,
    description             text,
    status                  varchar(32) not null default 'pending',
    initiated_by_user_id    bigint references users(id),
    created_at              timestamptz not null default now(),
    completed_at            timestamptz
);

create index idx_transactions_from_account_id on transactions(from_account_id);
create index idx_transactions_to_account_id on transactions(to_account_id);
create index idx_transactions_status on transactions(status);
create index idx_transactions_created_at on transactions(created_at);
create index idx_transactions_initiated_by_user_id on transactions(initiated_by_user_id);

create table transaction_authentications (
    id                  bigserial primary key,
    transaction_id      bigint not null references transactions(id),
    pin_verified        boolean not null default false,
    otp_code_hash       varchar(255),
    otp_verified        boolean not null default false,
    digital_signature   text,
    auth_status         varchar(32) not null default 'pending',
    verified_at         timestamptz
);

create index idx_transaction_auth_transaction_id on transaction_authentications(transaction_id);
create index idx_transaction_auth_status on transaction_authentications(auth_status);

create table transaction_categories (
    id                  bigserial primary key,
    transaction_id      bigint not null references transactions(id),
    category_code       varchar(50) not null,
    flow_type           varchar(20) not null,
    confidence          numeric(5,4),
    source              varchar(20) not null,
    tagged_by_user_id   bigint references users(id),
    tagged_at           timestamptz not null default now()
);

create index idx_transaction_categories_transaction_id on transaction_categories(transaction_id);
create index idx_transaction_categories_tagged_by_user_id on transaction_categories(tagged_by_user_id);

create table account_balance_ledger (
    id              bigserial primary key,
    account_id      bigint not null references accounts(id),
    transaction_id  bigint references transactions(id),
    entry_type      varchar(32) not null,
    amount          numeric(18,2) not null,
    balance_before  numeric(18,2) not null,
    balance_after   numeric(18,2) not null,
    created_at      timestamptz not null default now()
);

create index idx_account_balance_ledger_account_id on account_balance_ledger(account_id);
create index idx_account_balance_ledger_transaction_id on account_balance_ledger(transaction_id);
create index idx_account_balance_ledger_created_at on account_balance_ledger(created_at);

-- 5) Saving products & savings
create table saving_products (
    id                              bigserial primary key,
    code                            varchar(32) not null unique,
    name                            varchar(255) not null,
    currency                        varchar(10) not null default 'VND',

    term_unit                       varchar(16) not null,
    term_value                      int not null,

    interest_rate_type              varchar(16) not null,
    base_interest_rate              numeric(8,4) not null,
    penalty_interest_rate           numeric(8,4),
    bonus_interest_rate             numeric(8,4),

    interest_accrual_frequency      varchar(32) not null,
    interest_posting_frequency      varchar(32) not null,
    capitalized                     boolean not null default true,

    min_open_amount                 numeric(18,2) not null,
    max_open_amount                 numeric(18,2),

    deposit_fee_rate                numeric(14,4),
    deposit_fee_flat                numeric(18,2),
    withdrawal_fee_rate             numeric(14,4),
    withdrawal_fee_flat             numeric(18,2),
    close_fee_rate                  numeric(14,4),
    close_fee_flat                  numeric(18,2),
    management_fee_rate             numeric(14,4),
    management_fee_flat             numeric(18,2),
    management_fee_frequency        varchar(32),

    status                          varchar(32) not null default 'active',
    created_at                      timestamptz not null default now(),
    updated_at                      timestamptz not null default now()
);

create index idx_saving_products_status on saving_products(status);

create table saving_product_interest_tiers (
    id                  bigserial primary key,
    saving_product_id   bigint not null references saving_products(id),
    min_amount          numeric(18,2) not null,
    max_amount          numeric(18,2),
    interest_rate       numeric(8,4) not null,
    effective_from      date not null,
    effective_to        date,
    created_at          timestamptz not null default now()
);

create index idx_saving_product_interest_tiers_product_id on saving_product_interest_tiers(saving_product_id);
create index idx_saving_product_interest_tiers_effective_from on saving_product_interest_tiers(effective_from);

create table savings (
    id                              bigserial primary key,
    code                            varchar(32) not null unique,
    user_id                         bigint not null references users(id),
    saving_product_id               bigint not null references saving_products(id),

    source_account_id               bigint not null references accounts(id),
    settlement_account_id           bigint references accounts(id),

    principal_amount                numeric(18,2) not null,
    actual_interest_rate            numeric(8,4) not null,
    interest_rate_type              varchar(16) not null,
    penalty_interest_rate           numeric(8,4),
    bonus_interest_rate             numeric(8,4),

    interest_accrual_frequency      varchar(32) not null,
    interest_posting_frequency      varchar(32) not null,
    capitalized                     boolean not null default true,

    accrued_interest_amount         numeric(18,2) not null default 0,
    posted_interest_amount          numeric(18,2) not null default 0,
    projected_maturity_amount       numeric(18,2),

    deposit_amount_min              numeric(18,2),
    deposit_amount_max              numeric(18,2),
    deposit_fee_rate                numeric(14,4),
    deposit_fee_flat                numeric(18,2),

    withdrawal_amount_min           numeric(18,2),
    withdrawal_amount_max           numeric(18,2),
    withdrawal_fee_rate             numeric(14,4),
    withdrawal_fee_flat             numeric(18,2),

    entry_fee_rate                  numeric(14,4),
    entry_fee_flat                  numeric(18,2),
    close_fee_rate                  numeric(14,4),
    close_fee_flat                  numeric(18,2),
    management_fee_rate             numeric(14,4),
    management_fee_flat             numeric(18,2),
    management_fee_frequency        varchar(32),

    term_unit                       varchar(16) not null,
    term_value                      int not null,

    status                          varchar(32) not null,
    open_date                       timestamptz,
    maturity_date                   timestamptz,
    close_date                      timestamptz,
    auto_renew                      boolean not null default false,

    created_at                      timestamptz not null default now(),
    created_by_id                   bigint not null references admin_users(id),
    opened_by_id                    bigint references admin_users(id),
    closed_by_id                    bigint references admin_users(id),
    locked                          boolean not null default false
);

create index idx_savings_user_id on savings(user_id);
create index idx_savings_product_id on savings(saving_product_id);
create index idx_savings_source_account_id on savings(source_account_id);
create index idx_savings_settlement_account_id on savings(settlement_account_id);
create index idx_savings_status on savings(status);

create table savings_accounts (
    id          bigserial primary key,
    type        varchar(50) not null,
    saving_id   bigint not null references savings(id),
    account_id  bigint not null references accounts(id)
);

create index idx_savings_accounts_saving_id on savings_accounts(saving_id);
create index idx_savings_accounts_account_id on savings_accounts(account_id);

create table saving_transactions (
    id                  bigserial primary key,
    saving_id           bigint not null references savings(id),
    transaction_type    varchar(50) not null,
    transaction_id      bigint references transactions(id),
    amount              numeric(18,2) not null,
    interest_amount     numeric(18,2) not null default 0,
    fee_amount          numeric(18,2) not null default 0,
    description         text,
    created_at          timestamptz not null default now()
);

create index idx_saving_transactions_saving_id on saving_transactions(saving_id);
create index idx_saving_transactions_transaction_id on saving_transactions(transaction_id);
create index idx_saving_transactions_created_at on saving_transactions(created_at);

-- 6) Loan products & loans
create table loan_products (
    id                              bigserial primary key,
    code                            varchar(32) not null unique,
    name                            varchar(255) not null,
    loan_type                       varchar(32) not null,
    currency                        varchar(10) not null default 'VND',

    min_amount                      numeric(18,2) not null,
    max_amount                      numeric(18,2) not null,
    min_term_months                 int not null,
    max_term_months                 int not null,

    interest_rate_type              varchar(16) not null,
    base_interest_rate              numeric(8,4) not null,
    penalty_interest_rate           numeric(8,4),
    grace_interest_rate             numeric(8,4),

    processing_fee_rate             numeric(14,4),
    processing_fee_flat             numeric(18,2),
    early_repayment_fee_rate        numeric(14,4),
    early_repayment_fee_flat        numeric(18,2),

    interest_calculation_method     varchar(32) not null,
    repayment_frequency             varchar(32) not null,

    status                          varchar(32) not null default 'active',
    created_at                      timestamptz not null default now(),
    updated_at                      timestamptz not null default now()
);

create index idx_loan_products_status on loan_products(status);

create table loan_product_interest_tiers (
    id              bigserial primary key,
    loan_product_id bigint not null references loan_products(id),
    min_amount      numeric(18,2) not null,
    max_amount      numeric(18,2),
    min_term_months int,
    max_term_months int,
    interest_rate   numeric(8,4) not null,
    effective_from  date not null,
    effective_to    date,
    created_at      timestamptz not null default now()
);

create index idx_loan_product_interest_tiers_product_id on loan_product_interest_tiers(loan_product_id);
create index idx_loan_product_interest_tiers_effective_from on loan_product_interest_tiers(effective_from);

create table loan_applications (
    id                      bigserial primary key,
    user_id                 bigint not null references users(id),
    loan_product_id         bigint references loan_products(id),
    requested_amount        numeric(18,2) not null,
    requested_term_months   int not null,
    monthly_income          numeric(18,2),
    purpose                 text,
    collateral_description  text,
    priority_tag            varchar(32),
    status                  varchar(32) not null default 'pending',
    submitted_at            timestamptz not null default now(),
    reviewed_at             timestamptz,
    reviewed_by_id          bigint references admin_users(id),
    review_note             text
);

create index idx_loan_applications_user_id on loan_applications(user_id);
create index idx_loan_applications_loan_product_id on loan_applications(loan_product_id);
create index idx_loan_applications_status on loan_applications(status);
create index idx_loan_applications_reviewed_by_id on loan_applications(reviewed_by_id);

create table loans (
    id                              bigserial primary key,
    code                            varchar(32) not null unique,
    loan_application_id             bigint not null references loan_applications(id),
    user_id                         bigint not null references users(id),
    loan_product_id                 bigint references loan_products(id),
    disbursement_account_id         bigint references accounts(id),
    repayment_account_id            bigint references accounts(id),

    approved_amount                 numeric(18,2) not null,
    disbursed_amount                numeric(18,2) not null,

    interest_rate_type              varchar(16) not null,
    actual_interest_rate            numeric(8,4) not null,
    penalty_interest_rate           numeric(8,4),
    grace_interest_rate             numeric(8,4),

    processing_fee_rate             numeric(14,4),
    processing_fee_flat             numeric(18,2),
    early_repayment_fee_rate        numeric(14,4),
    early_repayment_fee_flat        numeric(18,2),

    interest_calculation_method     varchar(32) not null,
    repayment_frequency             varchar(32) not null,

    term_months                     int not null,
    outstanding_principal           numeric(18,2) not null,
    outstanding_interest            numeric(18,2) not null default 0,
    overdue_principal               numeric(18,2) not null default 0,
    overdue_interest                numeric(18,2) not null default 0,

    status                          varchar(32) not null,
    disbursed_at                    timestamptz,
    next_due_date                   timestamptz,
    closed_at                       timestamptz,
    created_at                      timestamptz not null default now()
);

create index idx_loans_loan_application_id on loans(loan_application_id);
create index idx_loans_user_id on loans(user_id);
create index idx_loans_loan_product_id on loans(loan_product_id);
create index idx_loans_disbursement_account_id on loans(disbursement_account_id);
create index idx_loans_repayment_account_id on loans(repayment_account_id);
create index idx_loans_status on loans(status);

create table loan_repayment_schedule (
    id                          bigserial primary key,
    loan_id                     bigint not null references loans(id),
    installment_no              int not null,
    due_date                    date not null,

    opening_principal_balance   numeric(18,2) not null,
    principal_due               numeric(18,2) not null,
    interest_rate               numeric(8,4) not null,
    interest_due                numeric(18,2) not null,
    penalty_interest_due        numeric(18,2) not null default 0,
    fee_due                     numeric(18,2) not null default 0,
    total_due                   numeric(18,2) not null,

    principal_paid              numeric(18,2) not null default 0,
    interest_paid               numeric(18,2) not null default 0,
    penalty_interest_paid       numeric(18,2) not null default 0,
    fee_paid                    numeric(18,2) not null default 0,

    status                      varchar(32) not null default 'unpaid',
    paid_at                     timestamptz
);

create index idx_loan_repayment_schedule_loan_id on loan_repayment_schedule(loan_id);
create index idx_loan_repayment_schedule_due_date on loan_repayment_schedule(due_date);
create index idx_loan_repayment_schedule_status on loan_repayment_schedule(status);
create unique index ux_loan_repayment_schedule_loan_installment on loan_repayment_schedule(loan_id, installment_no);

create table loan_repayments (
    id                          bigserial primary key,
    loan_id                     bigint not null references loans(id),
    repayment_schedule_id       bigint references loan_repayment_schedule(id),
    transaction_id              bigint references transactions(id),
    paid_amount                 numeric(18,2) not null,
    principal_component         numeric(18,2) not null default 0,
    interest_component          numeric(18,2) not null default 0,
    penalty_interest_component  numeric(18,2) not null default 0,
    fee_component               numeric(18,2) not null default 0,
    paid_at                     timestamptz not null default now()
);

create index idx_loan_repayments_loan_id on loan_repayments(loan_id);
create index idx_loan_repayments_repayment_schedule_id on loan_repayments(repayment_schedule_id);
create index idx_loan_repayments_transaction_id on loan_repayments(transaction_id);
create index idx_loan_repayments_paid_at on loan_repayments(paid_at);

-- 7) Service requests
create table service_requests (
    id              bigserial primary key,
    user_id         bigint not null references users(id),
    request_type    varchar(50) not null,
    priority_tag    varchar(32),
    status          varchar(32) not null default 'pending',
    title           varchar(255),
    description     text,
    payload_json    jsonb,
    submitted_at    timestamptz not null default now(),
    assigned_to_id  bigint references admin_users(id),
    processed_at    timestamptz,
    process_note    text
);

create index idx_service_requests_user_id on service_requests(user_id);
create index idx_service_requests_status on service_requests(status);
create index idx_service_requests_assigned_to_id on service_requests(assigned_to_id);

create table limit_change_requests (
    id                              bigserial primary key,
    service_request_id              bigint not null references service_requests(id),
    account_id                      bigint not null references accounts(id),
    current_daily_transfer_limit    numeric(18,2) not null,
    requested_daily_transfer_limit  numeric(18,2) not null,
    reason                          text
);

create index idx_limit_change_requests_service_request_id on limit_change_requests(service_request_id);
create index idx_limit_change_requests_account_id on limit_change_requests(account_id);

-- 8) Chat
create table chat_conversations (
    id                      bigserial primary key,
    user_id                 bigint not null references users(id),
    channel                 varchar(32) not null,
    status                  varchar(32) not null,
    assigned_admin_user_id  bigint references admin_users(id),
    started_at              timestamptz not null default now(),
    ended_at                timestamptz
);

create index idx_chat_conversations_user_id on chat_conversations(user_id);
create index idx_chat_conversations_status on chat_conversations(status);
create index idx_chat_conversations_assigned_admin_user_id on chat_conversations(assigned_admin_user_id);

create table chat_messages (
    id              bigserial primary key,
    conversation_id bigint not null references chat_conversations(id),
    sender_type     varchar(32) not null,
    sender_id       bigint,
    message_type    varchar(32) not null default 'text',
    content         text not null,
    created_at      timestamptz not null default now()
);

create index idx_chat_messages_conversation_id on chat_messages(conversation_id);
create index idx_chat_messages_created_at on chat_messages(created_at);

create table chat_notes (
    id              bigserial primary key,
    conversation_id bigint not null references chat_conversations(id),
    admin_user_id   bigint not null references admin_users(id),
    note            text not null,
    created_at      timestamptz not null default now()
);

create index idx_chat_notes_conversation_id on chat_notes(conversation_id);
create index idx_chat_notes_admin_user_id on chat_notes(admin_user_id);

-- 9) Logs & notifications
create table system_logs (
    id              bigserial primary key,
    actor_type      varchar(32) not null,
    actor_id        bigint,
    action          varchar(100) not null,
    target_type     varchar(50),
    target_id       bigint,
    metadata_json   jsonb,
    ip_address      varchar(64),
    created_at      timestamptz not null default now()
);

create index idx_system_logs_actor on system_logs(actor_type, actor_id);
create index idx_system_logs_action on system_logs(action);
create index idx_system_logs_created_at on system_logs(created_at);

create table notifications (
    id              bigserial primary key,
    user_id         bigint references users(id),
    channel         varchar(20) not null,
    type            varchar(50) not null,
    title           varchar(255),
    content         text not null,
    status          varchar(32) not null default 'pending',
    scheduled_at    timestamptz,
    sent_at         timestamptz,
    created_at      timestamptz not null default now()
);

create index idx_notifications_user_id on notifications(user_id);
create index idx_notifications_status on notifications(status);
create index idx_notifications_scheduled_at on notifications(scheduled_at);
