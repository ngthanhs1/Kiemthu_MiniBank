create table if not exists faq_categories (
    id          bigserial primary key,
    code        varchar(64) not null unique,
    name        varchar(120) not null,
    description text,
    sort_order  integer not null default 0,
    active      boolean not null default true,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

create table if not exists faq_items (
    id          bigserial primary key,
    category_id bigint not null references faq_categories(id),
    question    varchar(255) not null,
    answer      text not null,
    priority    integer not null default 100,
    active      boolean not null default true,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

create table if not exists faq_keywords (
    id                 bigserial primary key,
    faq_item_id        bigint not null references faq_items(id) on delete cascade,
    keyword            varchar(120) not null,
    normalized_keyword varchar(120) not null,
    created_at         timestamptz not null default now()
);

create index if not exists idx_faq_items_category_id on faq_items(category_id);
create index if not exists idx_faq_items_active_priority on faq_items(active, priority);
create index if not exists idx_faq_keywords_faq_item_id on faq_keywords(faq_item_id);
create index if not exists idx_faq_keywords_normalized on faq_keywords(normalized_keyword);

alter table chat_conversations add column if not exists last_intent varchar(120);
alter table chat_conversations add column if not exists last_confidence integer;
alter table chat_conversations add column if not exists escalated_at timestamptz;

insert into faq_categories(code, name, description, sort_order, active)
values
    ('GIAO_DICH', 'Giao dịch', 'Các vấn đề về chuyển khoản và giao dịch', 1, true),
    ('TIET_KIEM', 'Tiết kiệm', 'Hướng dẫn mở và quản lý tiết kiệm', 2, true),
    ('VAY_VON', 'Vay vốn', 'Tư vấn quy trình vay và hồ sơ', 3, true),
    ('TAI_KHOAN', 'Tài khoản', 'Thông tin tài khoản và hạn mức', 4, true),
    ('BAO_MAT', 'Bảo mật', 'OTP, PIN, an toàn tài khoản', 5, true)
on conflict (code) do nothing;

with selected_category as (
    select id from faq_categories where code = 'GIAO_DICH'
)
insert into faq_items(category_id, question, answer, priority, active)
select id, 'Chuyển khoản bị trừ tiền nhưng người nhận chưa nhận',
       'Bạn vui lòng kiểm tra trạng thái giao dịch trong lịch sử. Nếu quá 15 phút vẫn chưa hoàn tất, hãy chọn Gặp nhân viên hỗ trợ để tạo yêu cầu tra soát.',
       1,
       true
from selected_category
where not exists (
    select 1 from faq_items where question = 'Chuyển khoản bị trừ tiền nhưng người nhận chưa nhận'
);

with selected_category as (
    select id from faq_categories where code = 'GIAO_DICH'
)
insert into faq_items(category_id, question, answer, priority, active)
select id, 'Không nhận được mã OTP khi chuyển khoản',
       'Bạn hãy kiểm tra sóng điện thoại, tin nhắn chặn và thử gửi lại OTP sau 30 giây. Nếu vẫn không nhận được, vui lòng liên hệ CSKH để xác minh thuê bao.',
       2,
       true
from selected_category
where not exists (
    select 1 from faq_items where question = 'Không nhận được mã OTP khi chuyển khoản'
);

insert into faq_keywords(faq_item_id, keyword, normalized_keyword)
select item.id, keyword.keyword, keyword.normalized
from faq_items item
cross join (
    values
        ('chuyen khoan bi tru tien', 'chuyen khoan bi tru tien'),
        ('nguoi nhan chua nhan', 'nguoi nhan chua nhan'),
        ('chuyen khoan loi', 'chuyen khoan loi'),
        ('khong nhan otp', 'khong nhan otp'),
        ('otp khong ve', 'otp khong ve')
) as keyword(keyword, normalized)
where item.question in (
    'Chuyển khoản bị trừ tiền nhưng người nhận chưa nhận',
    'Không nhận được mã OTP khi chuyển khoản'
)
and not exists (
    select 1
    from faq_keywords existing
    where existing.faq_item_id = item.id
      and existing.normalized_keyword = keyword.normalized
);
