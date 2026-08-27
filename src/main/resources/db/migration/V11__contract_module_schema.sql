-- Align contract module schema with the current JPA entities.

alter table contract_templates
    add column if not exists services varchar(128) not null default 'general',
    add column if not exists status varchar(32) not null default 'draft',
    add column if not exists updated_by_id bigint references admin_users(id),
    add column if not exists updated_at timestamptz not null default now();

alter table contracts
    add column if not exists rendered_body text,
    add column if not exists updated_at timestamptz not null default now();

create table if not exists contract_template_placeholders (
    id bigserial primary key,
    contract_template_id bigint not null references contract_templates(id) on delete cascade,
    field_code varchar(100) not null,
    field_label varchar(255),
    data_source varchar(50),
    sort_order integer default 0,
    constraint uk_contract_template_placeholders_template_code unique (contract_template_id, field_code)
);

create index if not exists idx_contract_template_placeholders_template_id
    on contract_template_placeholders(contract_template_id);