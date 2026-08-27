alter table faq_items
    add column if not exists parent_faq_item_id bigint references faq_items(id) on delete set null;

create index if not exists idx_faq_items_parent_faq_item_id on faq_items(parent_faq_item_id);
