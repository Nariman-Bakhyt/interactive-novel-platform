Alter table novel
add column if not exists is_deleted boolean default false;

Alter table chapter
add column if not exists is_deleted boolean default false;

create table IF NOT EXISTS audit_log (
    id bigserial primary key ,
    action varchar(20) not null ,
    entity_type varchar(50) not null ,
    target_id bigint not null ,
    actor_id bigint not null ,
    reason varchar(100) not null ,
    details text ,
    timestamp timestamp with time zone default current_timestamp

);
CREATE INDEX IF NOT EXISTS idx_audit_log_target ON audit_log(entity_type, target_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_actor ON audit_log(actor_id);


ALTER TABLE chapter DROP CONSTRAINT IF EXISTS uk_novel_chapter_number;
CREATE UNIQUE INDEX IF NOT EXISTS  uk_novel_chapter_number_active ON chapter (novel_id, chapter_number)
    WHERE is_deleted = false;

DROP INDEX IF EXISTS idx_novel_chapter_count;
DROP INDEX IF EXISTS idx_novel_status;
DROP INDEX IF EXISTS idx_novel_view_count;
DROP INDEX IF EXISTS idx_novel_publication_date;
DROP INDEX IF EXISTS idx_novel_title_trgm;

CREATE INDEX IF NOT EXISTS idx_novel_chapter_count_active ON novel (chapter_count DESC) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_novel_status_active ON novel (status) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_novel_view_count_active ON novel (view_count DESC) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_novel_publication_date_active ON novel (publication_date DESC) WHERE is_deleted = false;
-- Твой триграммный поиск по названию
CREATE INDEX IF NOT EXISTS idx_novel_title_trgm_active ON novel USING gin (title gin_trgm_ops) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_comment_parent_active ON comment (parent_comment_id, timestamp)
    WHERE is_deleted = false;

ALTER TABLE message
    ADD COLUMN IF NOT EXISTS metadata jsonb DEFAULT NULL;
ALTER TABLE comment
    ADD COLUMN IF NOT EXISTS metadata jsonb DEFAULT NULL;

DROP INDEX IF EXISTS idx_comment_chapter;
DROP INDEX IF EXISTS idx_comment_forum;
DROP INDEX IF EXISTS idx_comment_novel;
DROP INDEX IF EXISTS idx_comment_parent;
CREATE INDEX IF NOT EXISTS idx_message_metadata_gin ON message USING gin (metadata);