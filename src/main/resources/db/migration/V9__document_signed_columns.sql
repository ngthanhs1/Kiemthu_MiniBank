-- Add signed tracking columns to documents
alter table documents
    add column if not exists signed_status varchar(32),
    add column if not exists signed_by_user_id bigint,
    add column if not exists signed_at timestamptz;

create index if not exists idx_documents_signed_by on documents(signed_by_user_id);
