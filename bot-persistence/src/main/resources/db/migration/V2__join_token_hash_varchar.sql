alter table lunch_rounds
    alter column join_token_hash type varchar(64)
    using rtrim(join_token_hash)::varchar(64);
