create table ai_settings (
    id                              bigserial primary key,
    classification_enabled           boolean not null default true,
    classification_frequency_minutes int not null default 60,
    classification_start_time        varchar(8),
    recommendation_enabled           boolean not null default true,
    recommendation_frequency_minutes int not null default 1440,
    recommendation_start_time        varchar(8),
    last_classification_run          timestamptz,
    last_recommendation_run          timestamptz,
    created_at                       timestamptz not null default now(),
    updated_at                       timestamptz not null default now()
);

insert into ai_settings (
    classification_enabled,
    classification_frequency_minutes,
    recommendation_enabled,
    recommendation_frequency_minutes
) values (true, 60, true, 1440);

create table ai_daily_recommendations (
    id                  bigserial primary key,
    user_id             bigint not null references users(id),
    day                 date not null,
    month               varchar(7) not null,
    risk_level          varchar(16) not null,
    saving_score        int not null,
    recommendations_json text,
    source              varchar(32) not null,
    created_at          timestamptz not null default now()
);

create unique index uq_ai_daily_recommendations_user_day
    on ai_daily_recommendations(user_id, day);
