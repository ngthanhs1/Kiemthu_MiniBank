-- Flyway migration: add contract templates and contracts

create table contract_templates (
    id              bigserial primary key,
    name            varchar(128) not null,
    code            varchar(64) not null unique,
    description     text,
    template_body   text,
    template_file_url text,
    created_by_id   bigint references admin_users(id),
    created_at      timestamptz not null default now()
);

create table contracts (
    id              bigserial primary key,
    owner_type      varchar(64) not null,
    owner_id        bigint not null,
    template_id     bigint references contract_templates(id),
    contract_number varchar(64),
    file_url        text,
    status          varchar(32) not null default 'DRAFT',
    signed_at       timestamptz,
    created_by_id   bigint references admin_users(id),
    created_at      timestamptz not null default now()
);

create index idx_contracts_owner on contracts(owner_type, owner_id);
