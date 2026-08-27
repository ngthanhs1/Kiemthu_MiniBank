create table approval_policies (
    id bigserial primary key,
    service_type varchar(50) not null,
    min_amount numeric(18,2) not null default 0,
    max_amount numeric(18,2),
    staff_approvals_required int not null default 1,
    manager_approvals_required int not null default 0,
    active boolean not null default true,
    description text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_approval_policies_service_type on approval_policies(service_type);
create index idx_approval_policies_active on approval_policies(active);

create table approval_instances (
    id bigserial primary key,
    service_type varchar(50) not null,
    target_id bigint not null,
    approval_policy_id bigint references approval_policies(id),
    status varchar(32) not null default 'PENDING',
    current_stage varchar(32) not null default 'STAFF',
    staff_approvals_required int not null default 1,
    manager_approvals_required int not null default 0,
    staff_approved_count int not null default 0,
    manager_approved_count int not null default 0,
    created_at timestamptz not null default now(),
    completed_at timestamptz,
    unique (service_type, target_id)
);

create index idx_approval_instances_target on approval_instances(service_type, target_id);
create index idx_approval_instances_status on approval_instances(status);

create table approval_actions (
    id bigserial primary key,
    approval_instance_id bigint not null references approval_instances(id),
    admin_user_id bigint not null references admin_users(id),
    approver_role varchar(32) not null,
    action varchar(32) not null,
    note text,
    acted_at timestamptz not null default now()
);

create index idx_approval_actions_instance_id on approval_actions(approval_instance_id);
create index idx_approval_actions_admin_user_id on approval_actions(admin_user_id);

insert into approval_policies (
    service_type, min_amount, max_amount, staff_approvals_required, manager_approvals_required, active, description
) values
    ('saving', 0, 100000000, 1, 0, true, 'Tiết kiệm: một nhân viên duyệt'),
    ('saving', 100000000, null, 2, 1, true, 'Tiết kiệm: hai nhân viên và một quản lý'),
    ('loan_application', 0, 100000000, 1, 0, true, 'Vay vốn: một nhân viên duyệt'),
    ('loan_application', 100000000, null, 2, 1, true, 'Vay vốn: hai nhân viên và một quản lý')
on conflict do nothing;
