CREATE TABLE chapter_blocks (
                                id BIGSERIAL PRIMARY KEY,
                                chapter_id BIGINT NOT NULL,
                                sequence_order INTEGER NOT NULL,
                                type VARCHAR(20) NOT NULL, -- TEXT, IMAGE, GIF
                                content TEXT NOT NULL,
                                CONSTRAINT fk_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE
);

CREATE INDEX idx_chapter_blocks_order ON chapter_blocks(chapter_id, sequence_order);

ALTER TABLE chapter DROP COLUMN content;

-- 1. Удаляем старый целочисленный индекс (он нам больше не нужен)
ALTER TABLE comment DROP COLUMN IF EXISTS paragraph_index;

-- 2. Добавляем колонку для точечной привязки к конкретному блоку (абзацу/картинке)
ALTER TABLE comment ADD COLUMN block_id BIGINT;

-- 3. Устанавливаем внешний ключ на новую таблицу блоков
ALTER TABLE comment
    ADD CONSTRAINT fk_comment_chapter_block
        FOREIGN KEY (block_id) REFERENCES chapter_blocks(id) ON DELETE CASCADE;