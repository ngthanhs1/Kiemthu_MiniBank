alter table faq_items
    drop column if exists priority;

drop index if exists idx_faq_items_active_priority;

create index if not exists idx_faq_items_active_created_at on faq_items(active, created_at desc);
