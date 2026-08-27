create table permission_definitions (
    id bigserial primary key,
    code varchar(80) not null unique,
    label varchar(160) not null,
    tab_group varchar(120) not null,
    description text,
    sort_order int not null default 100,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_permission_definitions_tab_group on permission_definitions(tab_group);
create index idx_permission_definitions_active on permission_definitions(active);

insert into permission_definitions (code, label, tab_group, description, sort_order, active) values
    ('CUSTOMER_VIEW', 'Xem danh sách khách hàng', 'Khách hàng', 'Xem danh sách khách hàng và thông tin cơ bản', 10, true),
    ('CUSTOMER_KYC_VIEW', 'Xem hồ sơ KYC', 'Khách hàng', 'Xem chi tiết hồ sơ KYC của khách hàng', 20, true),
    ('CUSTOMER_KYC_APPROVE', 'Phê duyệt KYC', 'Khách hàng', 'Phê duyệt hồ sơ KYC hợp lệ', 30, true),
    ('CUSTOMER_KYC_REJECT', 'Từ chối KYC', 'Khách hàng', 'Từ chối hồ sơ KYC không hợp lệ', 40, true),
    ('CUSTOMER_DOCUMENT_VIEW', 'Xem tài liệu khách hàng', 'Khách hàng', 'Xem tài liệu định danh và hồ sơ đính kèm', 50, true),
    ('TRANSACTION_CLASSIFY', 'Phân loại giao dịch', 'Tài khoản & Giao dịch', 'Phân loại giao dịch phát sinh', 10, true),
    ('TRANSACTION_LARGE_APPROVAL', 'Duyệt giao dịch lớn', 'Tài khoản & Giao dịch', 'Phê duyệt hoặc từ chối giao dịch giá trị lớn', 20, true),
    ('SAVING_PRODUCT_MANAGE', 'Quản lý sản phẩm tiết kiệm', 'Sản phẩm tài chính', 'Tạo và chỉnh sửa sản phẩm tiết kiệm', 10, true),
    ('SAVING_TIER_MANAGE', 'Quản lý bậc tiết kiệm', 'Sản phẩm tài chính', 'Cấu hình bậc lãi suất tiết kiệm', 20, true),
    ('SAVING_ACCOUNT_VIEW', 'Xem sổ tiết kiệm', 'Sản phẩm tài chính', 'Xem danh sách và chi tiết sổ tiết kiệm', 30, true),
    ('SAVING_APPROVAL', 'Duyệt sổ tiết kiệm', 'Yêu cầu thủ tục', 'Duyệt các yêu cầu mở/tất toán tiết kiệm', 10, true),
    ('LOAN_PRODUCT_MANAGE', 'Quản lý sản phẩm vay', 'Sản phẩm tài chính', 'Tạo và chỉnh sửa sản phẩm vay', 40, true),
    ('LOAN_TIER_MANAGE', 'Quản lý bậc vay', 'Sản phẩm tài chính', 'Cấu hình bậc lãi suất vay', 50, true),
    ('LOAN_APPLICATION_APPROVAL', 'Duyệt vay vốn', 'Yêu cầu thủ tục', 'Duyệt hồ sơ vay vốn', 20, true),
    ('LIMIT_REQUEST_APPROVAL', 'Duyệt yêu cầu tăng hạn mức', 'Yêu cầu thủ tục', 'Duyệt yêu cầu tăng hạn mức của khách hàng', 30, true),
    ('PROFILE_REQUEST_APPROVAL', 'Duyệt yêu cầu đổi thông tin', 'Yêu cầu thủ tục', 'Duyệt yêu cầu đổi thông tin cá nhân', 40, true),
    ('CHAT_CONVERSATION_MANAGE', 'Quản lý chat CSKH', 'Hỗ trợ khách hàng', 'Nhận và xử lý cuộc chat CSKH', 10, true),
    ('FAQ_MANAGE', 'Quản lý FAQ', 'Hỗ trợ khách hàng', 'Thêm, sửa, xóa cây FAQ', 20, true),
    ('CONTRACT_TEMPLATE_MANAGE', 'Quản lý template hợp đồng', 'Hợp đồng & Thỏa thuận', 'Thêm, sửa, xóa template hợp đồng', 10, true),
    ('CONTRACT_LIST_VIEW', 'Xem tài liệu đã sinh', 'Hợp đồng & Thỏa thuận', 'Xem danh sách tài liệu hợp đồng đã sinh', 20, true),
    ('STAFF_MANAGE', 'Quản lý nhân viên', 'Quản trị hệ thống', 'Tạo, sửa, xóa và gán quyền nhân viên', 10, true),
    ('ROLE_MANAGE', 'Quản lý vai trò', 'Quản trị hệ thống', 'Tạo, sửa, xóa vai trò', 20, true),
    ('PERMISSION_MANAGE', 'Quản lý quyền', 'Quản trị hệ thống', 'Tạo, sửa, xóa quyền và tab quyền', 30, true),
    ('APPROVAL_POLICY_MANAGE', 'Quản lý mức duyệt nghiệp vụ', 'Quản trị hệ thống', 'Tạo, sửa, xóa cấu hình duyệt đa bước', 40, true),
    ('SYSTEM_AUDIT_VIEW', 'Xem nhật ký hệ thống', 'Quản trị hệ thống', 'Xem nhật ký và sự kiện hệ thống', 50, true)
on conflict (code) do update set
    label = excluded.label,
    tab_group = excluded.tab_group,
    description = excluded.description,
    sort_order = excluded.sort_order,
    active = excluded.active,
    updated_at = now();