DO $$
    BEGIN
        IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'user_relations') THEN
            ALTER TABLE user_relations RENAME TO user_followers;
        END IF;
END $$;

ALTER TABLE user_followers
    DROP IF EXISTS status ;

drop index if exists idx_user_relations_receiver ;

create index if not exists idx_followers_sender on user_followers(sender_id);
create index if not exists idx_followers_receiver on user_followers(receiver_id);

create table if not exists user_friends(
    ID bigserial primary key ,
    sender_id bigint not null references app_user(id) on delete cascade  ,
    receiver_id bigint not null references app_user(id) on delete cascade ,
    status varchar(50) not null ,

    updated_at timestamp with time zone default  current_timestamp,
    unique(sender_id,receiver_id),
    check ( sender_id <> receiver_id )
);
CREATE INDEX IF NOT EXISTS idx_friends_receiver ON user_friends(receiver_id);