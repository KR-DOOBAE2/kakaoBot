alter table office_profile add column lunch_end_time time;
update office_profile set lunch_end_time = (lunch_time + interval '1 hour')::time;
alter table office_profile alter column lunch_end_time set not null;
