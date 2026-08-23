-- Добавляем колонку статуса в таблицу chapter
ALTER TABLE chapter
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'DRAFT' NOT NULL,
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE NOT NULL
;

-- Для существующих данных: если у главы была заполнена дата публикации, переводим ее в PUBLISHED
UPDATE chapter SET status = 'PUBLISHED' WHERE published_at IS NOT NULL;

-- Удаляем старые индексы, которые не учитывали мягкое удаление
DROP INDEX IF EXISTS idx_chapter_title_upper;
DROP INDEX IF EXISTS idx_chapter_number;

-- Создаем новые оптимизированные индексы, исключающие удаленные главы
CREATE INDEX IF NOT EXISTS idx_chapter_title_upper_active ON chapter (UPPER(title)) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_chapter_number_active ON chapter (chapter_number) WHERE is_deleted = false;

-- Добавляем новые индексы под нашу логику статусов и фонового планировщика
CREATE INDEX IF NOT EXISTS idx_chapter_status_published_active ON chapter (status, published_at) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_chapter_novel_status_active ON chapter (novel_id, status) WHERE is_deleted = false;
