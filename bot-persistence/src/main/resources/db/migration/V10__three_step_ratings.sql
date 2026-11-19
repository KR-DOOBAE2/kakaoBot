alter table restaurant_ratings add column sentiment varchar(16);
update restaurant_ratings set sentiment=case when liked then 'LIKE' else 'DISLIKE' end;
alter table restaurant_ratings alter column sentiment set not null;
