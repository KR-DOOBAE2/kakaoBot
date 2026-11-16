create table lunch_notification_subscribers (
    bot_user_key varchar(200) primary key,
    enabled boolean not null default true,
    created_at timestamptz not null default now()
);
create table lunch_notification_sent (
    lunch_date date primary key,
    sent_at timestamptz not null default now()
);
