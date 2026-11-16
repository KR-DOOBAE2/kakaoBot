create table office_profile (
    id varchar(20) primary key,
    address varchar(240) not null,
    latitude double precision not null,
    longitude double precision not null,
    lunch_time time not null,
    radius_meters integer not null check (radius_meters between 100 and 20000),
    admin_bot_user_key varchar(200) not null
);
create table restaurants (
    place_id varchar(80) primary key,
    name varchar(160) not null,
    address varchar(300) not null,
    category varchar(160),
    phone varchar(80),
    place_url varchar(500),
    latitude double precision not null,
    longitude double precision not null,
    distance_meters integer not null default 0,
    first_seen_at timestamptz not null default now(),
    last_recommended_at timestamptz
);
create table restaurant_poll_candidates (
    id uuid primary key default gen_random_uuid(),
    poll_date date not null,
    place_id varchar(80) not null references restaurants(place_id),
    display_order integer not null,
    constraint uq_poll_place unique(poll_date, place_id)
);
create table restaurant_votes (
    id uuid primary key default gen_random_uuid(),
    poll_date date not null,
    participant_id uuid not null,
    place_id varchar(80) not null references restaurants(place_id),
    voted_at timestamptz not null default now(),
    constraint uq_poll_voter unique(poll_date, participant_id)
);
create table restaurant_ratings (
    id uuid primary key default gen_random_uuid(),
    participant_id uuid not null,
    place_id varchar(80) not null references restaurants(place_id),
    liked boolean not null,
    updated_at timestamptz not null default now(),
    constraint uq_restaurant_rater unique(participant_id, place_id)
);
create index ix_votes_day_place on restaurant_votes(poll_date,place_id);
create index ix_ratings_place on restaurant_ratings(place_id,liked);
create table restaurant_daily_winners (
    poll_date date primary key,
    place_id varchar(80) not null references restaurants(place_id),
    selected_at timestamptz not null default now()
);

