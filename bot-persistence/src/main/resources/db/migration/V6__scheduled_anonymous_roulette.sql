alter table lunch_rounds add column draw_at timestamptz;
create index ix_rounds_due on lunch_rounds(draw_at) where status = 'OPEN';
update menu_entries set participant_name = '익명';
