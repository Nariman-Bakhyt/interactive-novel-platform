ALTER TABLE novel
    ADD COLUMN average_rating NUMERIC(3,2) DEFAULT 0.00 NOT NULL,
    ADD COLUMN last_chapter_added_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE chapter
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ADD COLUMN published_at TIMESTAMP WITH TIME ZONE;

UPDATE novel
SET average_rating = CASE
                         WHEN rating_count > 0 THEN ROUND(total_score::numeric / rating_count, 2)
                         ELSE 0.00
    END,
    last_chapter_added_at = created_at;

DROP INDEX IF EXISTS idx_novel_chapter_count_active;
DROP INDEX IF EXISTS idx_novel_publication_date_active;
DROP INDEX IF EXISTS idx_novel_view_count_active;

CREATE INDEX idx_novel_chapter_count_active ON novel (chapter_count DESC, id) WHERE is_deleted = false;
CREATE INDEX idx_novel_publication_date_active ON novel (publication_date DESC, id) WHERE is_deleted = false;
CREATE INDEX idx_novel_view_count_active ON novel (view_count DESC, id) WHERE is_deleted = false;

CREATE INDEX idx_novel_average_rating_active ON novel (average_rating DESC, id) WHERE is_deleted = false;
CREATE INDEX idx_novel_last_chapter_active ON novel (last_chapter_added_at DESC, id) WHERE is_deleted = false;


CREATE INDEX idx_novel_tag_reverse ON novel_tag (tag_id, novel_id);
-- Предполагаю, что таблица жанров называется novel_genre
CREATE INDEX idx_novel_genre_reverse ON novel_genre (genre_id, novel_id);