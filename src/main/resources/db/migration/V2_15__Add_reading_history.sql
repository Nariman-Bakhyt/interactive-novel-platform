create table if not exists reading_history(
    user_id bigint not null references app_user(id)  on delete cascade ,
    novel_id bigint not null references novel(id) on delete cascade ,
    last_chapter_id bigint not null references chapter(id) on delete cascade,
    updated_at timestamp with time zone default current_timestamp ,
    primary key (user_id, novel_id)
);

CREATE INDEX if not exists idx_reading_history_user_updated ON reading_history (user_id, updated_at DESC);
CREATE INDEX if not exists idx_comment_novel_time ON comment (novel_id, timestamp DESC)
    WHERE is_deleted = FALSE;
CREATE INDEX if not exists idx_comment_chapter_time ON comment (chapter_id, timestamp DESC)
    WHERE is_deleted = FALSE;