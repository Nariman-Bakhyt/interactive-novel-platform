alter table user_library
    add column if not exists created_at timestamp with time zone default current_timestamp not null;

create index if not exists idx_user_library_created_at on user_library (user_id, created_at desc);