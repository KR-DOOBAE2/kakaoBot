create table round_satisfaction (
    id uuid primary key,
    round_id uuid not null references lunch_rounds(id) on delete cascade,
    rated_on date not null,
    participant_id uuid not null,
    satisfaction varchar(16) not null,
    updated_at timestamptz not null,
    constraint uq_round_satisfaction_participant unique (round_id,rated_on,participant_id)
);
create index idx_round_satisfaction_day on round_satisfaction(round_id,rated_on);
