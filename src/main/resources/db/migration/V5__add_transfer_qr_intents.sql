create table qr_transfer_intents (
    id                      bigserial primary key,
    intent_token            varchar(64) not null unique,
    account_id              bigint not null references accounts(id),
    amount                  numeric(18,2) not null,
    status                  varchar(32) not null default 'active',
    created_by_user_id      bigint references users(id),
    claimed_by_user_id      bigint,
    completed_transaction_id bigint,
    payload                 text not null,
    expires_at              timestamptz not null,
    claimed_at              timestamptz,
    completed_at            timestamptz,
    created_at              timestamptz not null default now()
);

create index idx_qr_transfer_intents_account_id on qr_transfer_intents(account_id);
create index idx_qr_transfer_intents_status on qr_transfer_intents(status);
create index idx_qr_transfer_intents_expires_at on qr_transfer_intents(expires_at);

alter table transactions
    add column qr_transfer_intent_id bigint unique references qr_transfer_intents(id);

create index idx_transactions_qr_transfer_intent_id on transactions(qr_transfer_intent_id);