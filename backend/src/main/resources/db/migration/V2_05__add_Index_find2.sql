ALTER TABLE chapter ALTER COLUMN chapter_number TYPE DOUBLE PRECISION;
-- 1. Индекс для названий новелл
CREATE INDEX idx_novel_title_upper ON novel (UPPER(title));
-- 2. Индекс для названий глав
CREATE INDEX idx_chapter_title_upper ON chapter (UPPER(title));
-- 3. Индекс для порядка глав (Double)
CREATE INDEX idx_chapter_number ON chapter (chapter_number);
-- 4. Индекс для названий групп
CREATE INDEX idx_group_name_upper ON chat_group (UPPER(name));
-- 5. Индекс для названий каналов
CREATE INDEX idx_channel_name_upper ON channel (UPPER(name));
-- 6. Индекс для названий форумов
CREATE INDEX idx_forum_name_upper ON forum_topic (UPPER(title));