alter table menu_entries drop constraint uq_round_participant;
create unique index uq_entries_participant_menu_ci on menu_entries(round_id, participant_id, lower(menu));
