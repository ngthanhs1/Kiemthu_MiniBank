-- Add approval tracking columns for savings
alter table if exists savings
    add column if not exists rejection_reason text,
    add column if not exists reviewed_by_id bigint references admin_users(id),
    add column if not exists reviewed_at timestamptz;

comment on column savings.rejection_reason is 'Lý do từ chối, null nếu được duyệt';
comment on column savings.reviewed_by_id is 'Admin đã duyệt hoặc từ chối';
comment on column savings.reviewed_at is 'Thời điểm admin xử lý';