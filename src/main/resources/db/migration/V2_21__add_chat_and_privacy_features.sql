ALTER TABLE  IF EXISTS user_settings
    ADD COLUMN IF NOT EXISTS communication_privacy VARCHAR(50) DEFAULT 'NOBODY';

ALTER TABLE IF EXISTS conversations
    ADD COLUMN IF NOT EXISTS last_message_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_message_preview VARCHAR(255),
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE;

ALTER TABLE IF EXISTS conversation_members
    ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_muted BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS last_read_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_conversation_members_sort ON conversation_members(user_id, is_pinned);

DROP TABLE IF EXISTS channel CASCADE;
DROP TABLE IF EXISTS forum_topic CASCADE;

CREATE TABLE IF NOT EXISTS subscribable_entities (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS forum_topic (
    id BIGINT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT, -- Описание или текст первого поста
    author_id BIGINT NOT NULL, -- Создатель темы

    is_locked BOOLEAN DEFAULT FALSE, -- Закрыта ли тема для ответов
    view_count BIGINT DEFAULT 0, -- Количество просмотров

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_forum_subscribable
    FOREIGN KEY (id) REFERENCES subscribable_entities(id) ON DELETE CASCADE,

    -- Добавили связь с автором
    CONSTRAINT fk_forum_author
    FOREIGN KEY (author_id) REFERENCES app_user(id) ON DELETE CASCADE
);

-- 5. Создание таблицы каналов
CREATE TABLE IF NOT EXISTS channel (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT, -- Описание канала ("О нас")
    avatar_url VARCHAR(500), -- Аватарка канала

    is_private BOOLEAN DEFAULT FALSE, -- Приватный или публичный

    author_id BIGINT NOT NULL, -- Владелец канала
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_channel_subscribable
    FOREIGN KEY (id) REFERENCES subscribable_entities(id) ON DELETE CASCADE,

    CONSTRAINT fk_channel_author
    FOREIGN KEY (author_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    target_id BIGINT NOT NULL REFERENCES subscribable_entities(id) ON DELETE CASCADE,

    target_type VARCHAR(50) NOT NULL, -- Чтобы Java знала, к какому классу кастить

    is_pinned BOOLEAN DEFAULT FALSE,
    is_muted BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,

    last_read_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_user_target_subscription UNIQUE (user_id, target_id)
);








