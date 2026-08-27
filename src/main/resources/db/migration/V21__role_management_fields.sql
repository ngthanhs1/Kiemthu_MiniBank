alter table roles add column if not exists color varchar(32);
alter table roles add column if not exists permissions_json text;
