create table if not exists user_relations (
    id bigserial primary key ,
    sender_id bigint not null references app_user(id) on delete cascade  ,
    receiver_id bigint not null references app_user(id) on delete cascade ,
    status varchar(50) not null ,
    updated_at timestamp with time zone default  current_timestamp,
    unique(sender_id,receiver_id),
    check ( sender_id <> receiver_id )
);

create table if not exists user_library(
    user_id bigint not null references app_user(id) on delete cascade ,
    novel_id bigint not null references novel(id) on delete cascade ,
    primary key (user_id,novel_id),
    status varchar(50) not null ,
    is_private boolean default true
);

create table if not exists user_settings(
    user_id bigint primary key references app_user(id) on delete cascade ,
    can_send_message varchar(50) default 'FRIENDS',
    show_library boolean default false
);

create table if not exists user_blocks(
    id bigserial primary key ,
    blocker_id bigint not null references app_user(id) on delete cascade ,
    blocked_id bigint not null references app_user(id) on delete cascade ,
    created_at timestamp with time zone default current_timestamp,
    unique (blocker_id,blocked_id),
    check (blocker_id <> blocked_id  )
);

CREATE INDEX IF NOT EXISTS idx_app_user_active
    ON app_user (id)
    WHERE is_deleted = false;

drop index if exists idx_app_user_email_upper;
drop index if exists idx_app_user_username_upper;
drop index if exists idx_user_username_trgm;

CREATE INDEX IF NOT EXISTS idx_app_user_username_upper
    ON app_user (upper(username::text))
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_app_user_email_upper
    ON app_user (upper(email::text))
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_user_username_trgm
    ON app_user USING gin (username gin_trgm_ops)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_user_relations_receiver
    ON user_relations (receiver_id);

CREATE INDEX IF NOT EXISTS idx_user_library_novel
    ON user_library (novel_id);