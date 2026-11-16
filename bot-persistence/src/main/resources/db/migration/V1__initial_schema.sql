create table lunch_rounds (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null,
    title varchar(80) not null,
    room_name varchar(80) not null,
    join_token_hash char(64) not null,
    status varchar(12) not null check (status in ('OPEN','DRAWN','CLOSED')),
    winner varchar(80),
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index ix_rounds_created on lunch_rounds(created_at desc);
create table menu_entries (
    id uuid primary key default gen_random_uuid(),
    round_id uuid not null references lunch_rounds(id) on delete cascade,
    participant_id uuid not null,
    participant_name varchar(40) not null,
    menu varchar(80) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_round_participant unique(round_id,participant_id)
);
create index ix_entries_round on menu_entries(round_id);
create index ix_entries_menu_ci on menu_entries(round_id, lower(menu));


