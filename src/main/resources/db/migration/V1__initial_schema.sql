CREATE TABLE role(
    id BIGSERIAL PRIMARY KEY ,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE app_user(
    id BIGSERIAL PRIMARY KEY ,
    username VARCHAR(50) NOT NULL UNIQUE ,
    password_hash varchar(255) NOT NULL ,
    email varchar(255) NOT NULL UNIQUE ,
    registration_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    role_id BIGINT NOT NULL ,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_user_role
                     FOREIGN KEY (role_id)
                     REFERENCES role (id)
                     ON DELETE RESTRICT
);

CREATE TABLE genre(
    id BIGSERIAL PRIMARY KEY ,
    name VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE tag(
    id BIGSERIAL PRIMARY KEY ,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE novel(
    id BIGSERIAL PRIMARY KEY ,
    title VARCHAR(255) NOT NULL ,
    status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS', --COMPLETED, IN_PROGRESS, HIATUS
    description TEXT,
    publication_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    chapter_count INTEGER DEFAULT 0 NOT NULL,
    average_rating NUMERIC(3, 2) DEFAULT 0.00 NOT NULL, -- 3 знака, 2 после запятой (например, 5.00)
    rating_count INTEGER DEFAULT 0 NOT NULL,
    author_id BIGINT NOT NULL ,
    view_count BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT fk_novel_author
                  FOREIGN KEY (author_id)
                  REFERENCES app_user (id)
                  ON DELETE RESTRICT

);
CREATE INDEX idx_novel_chapter_count ON novel (chapter_count DESC);
CREATE INDEX idx_novel_avg_rating ON novel (average_rating DESC);
CREATE INDEX idx_novel_view_count ON novel (view_count DESC);
CREATE INDEX idx_novel_status ON novel (status, );

CREATE TABLE novel_genre (
    novel_id BIGINT NOT NULL REFERENCES novel (id) ON DELETE CASCADE,
    genre_id BIGINT NOT NULL REFERENCES genre (id) ON DELETE CASCADE,

    PRIMARY KEY (novel_id, genre_id)
);
CREATE INDEX idx_genre_novel ON novel_genre (genre_id, novel_id);

CREATE TABLE novel_tag (
    novel_id BIGINT NOT NULL REFERENCES novel (id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tag (id) ON DELETE CASCADE,

    PRIMARY KEY (novel_id, tag_id)
);
CREATE INDEX idx_tag_novel ON novel_tag (tag_id, novel_id);

CREATE TABLE chapter
(
    id BIGSERIAL PRIMARY KEY ,
    novel_id BIGINT NOT NULL ,
    chapter_number INTEGER NOT NULL ,
    title VARCHAR(255) NOT NULL ,
    content TEXT NOT NULL ,

    CONSTRAINT fk_chapter_novel
                      FOREIGN KEY (novel_id)
                      REFERENCES novel (id)
                      ON DELETE CASCADE ,
    UNIQUE (novel_id, chapter_number)
);

CREATE TABLE message(
    id BIGSERIAL PRIMARY KEY ,
    sender_id BIGINT NOT NULL ,
    receiver_id BIGINT NOT NULL ,
    content TEXT NOT NULL ,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP ,


    CONSTRAINT fk_message_user_sender
                    FOREIGN KEY (sender_id)
                    REFERENCES app_user (id)
                    ON DELETE RESTRICT ,
    CONSTRAINT fk_message_user_receiver
                    FOREIGN KEY (receiver_id)
                    REFERENCES app_user (id)
                    ON DELETE RESTRICT

);
CREATE INDEX idx_message_participants ON message (sender_id, receiver_id, timestamp);-- для поиска сообщений от отправителя к получателю.
CREATE INDEX idx_message_participants_sym ON message (receiver_id, sender_id, timestamp);-- для поиска сообщений от получателя к отправителю.
-- не забуть добавить при объединении проверку по id чтобы не было коллизий ORDER BY timestamp DESC, id DESC

CREATE TABLE comment(
    id BIGSERIAL PRIMARY KEY ,
    user_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT, -- SET NULL: сохраняет комментарий, если пользователь удален (Soft Delete)
    content TEXT NOT NULL ,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Привязка к ветвлению (для ответов)
    parent_comment_id BIGINT REFERENCES comment (id) ON DELETE CASCADE,

    -- Привязка к Новелле (для общего обсуждения или форума новеллы)
    novel_id BIGINT REFERENCES novel (id) ON DELETE CASCADE,

    -- Привязка к Главе (для обсуждения под главой)
    chapter_id BIGINT REFERENCES chapter (id) ON DELETE CASCADE,

    -- Точечная привязка (для комментариев к абзацам)
    paragraph_index INTEGER,
    -- Комментарий к общей теме
    forum_topic_id BIGINT REFERENCES forum_topic (id) ON DELETE CASCADE,
    -- Комментарий к посту канала
    channel_post_id BIGINT REFERENCES channel_post (id) ON DELETE CASCADE,

    -- Ограничение: Комментарий должен быть привязан либо к новелле, либо к главе, либо быть ответом.
    -- Это усложнит SQL, но обеспечит целостность на уровне бизнес-логики.
    -- NOTE: В реальной БД эта сложная проверка часто делается в коде приложения.

    -- Пример: Комментарий к абзацу (paragraph_index IS NOT NULL) возможен только
    -- если chapter_id IS NOT NULL, но novel_id IS NULL.

    -- Для простоты и гибкости ВКР: проверку CHK_ONE_PARENT лучше делать в Java.
    -- UNIQUE (novel_id, chapter_id, parent_comment_id, paragraph_index) – нет, т.к. много комментов
);
CREATE INDEX idx_comment_novel ON comment (novel_id, timestamp);
CREATE INDEX idx_comment_chapter ON comment (chapter_id, timestamp);
CREATE INDEX idx_comment_forum ON comment (forum_topic_id, timestamp);
CREATE INDEX idx_comment_channel_post ON comment (channel_post_id, timestamp);
CREATE INDEX idx_comment_parent ON comment (parent_comment_id, timestamp);
CREATE INDEX idx_comment_paragraph ON comment (novel_id, chapter_id, paragraph_index);

CREATE TABLE rating(
    id BIGSERIAL PRIMARY KEY ,
    user_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    novel_id BIGINT NOT NULL REFERENCES novel (id) ON DELETE CASCADE,
    score INTEGER NOT NULL CHECK (score >= 1 AND score <= 5), -- Сама оценка (например, от 1 до 5)
    comment_text TEXT, -- Поле для отзыва/комментария к этой оценке (может быть NULL)
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Главное правило: Один пользователь — одна оценка на новеллу
    UNIQUE (user_id, novel_id)
);

-- 1. Контейнер группы
CREATE TABLE chat_group (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Связь Пользователь <-> Группа
CREATE TABLE group_member (
    group_id BIGINT NOT NULL REFERENCES chat_group (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'SUBSCRIBER', -- MODERATOR, SUBSCRIBER
    PRIMARY KEY (group_id, user_id)
);
CREATE INDEX idx_group_member_user ON group_member (user_id);
CREATE INDEX idx_group_member_role ON group_member (group_id, role);

-- 3. Сообщения в Группе (Сообщения теперь привязаны к группе)
CREATE TABLE group_message (
    id BIGSERIAL PRIMARY KEY ,
    sender_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    group_id BIGINT NOT NULL REFERENCES chat_group (id) ON DELETE CASCADE,
    content TEXT NOT NULL ,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_group_msg_history ON group_message (group_id, timestamp);--для поиска всех сообщений группы хронологически
CREATE INDEX idx_group_msg_sender ON group_message (sender_id,timestamp); --для поиска по отправителю все сообщения хронологически
-- не забуть добавить при объединении проверку по id message чтобы не было коллизий


-- Таблица для Общей Темы Форума
CREATE TABLE forum_topic (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    -- Привязка к создателю
    creator_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    -- Время создания темы
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--Индекс для Сортировки по Новизне
CREATE INDEX idx_forum_topic_created ON forum_topic (created_at DESC);

-- Контейнер Канала
CREATE TABLE channel (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    creator_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Членство / Роли в Канале
CREATE TABLE channel_member (
    channel_id BIGINT NOT NULL REFERENCES channel (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'SUBSCRIBER', -- MODERATOR, SUBSCRIBER
    PRIMARY KEY (channel_id, user_id)
);
CREATE INDEX idx_channel_member_user ON channel_member (user_id); -- Для быстрого поиска каналов пользователя
CREATE INDEX idx_channel_member_role ON channel_member (channel_id, role);

-- Сообщения Канала (Посты)
CREATE TABLE channel_post (
    id BIGSERIAL PRIMARY KEY,
    channel_id BIGINT NOT NULL REFERENCES channel (id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    can_comment BOOLEAN NOT NULL DEFAULT TRUE,
    can_react BOOLEAN NOT NULL DEFAULT TRUE,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
-- Индекс для загрузки истории канала (поиск по ID канала и сортировка по времени)
CREATE INDEX idx_channel_post_history ON channel_post (channel_id, timestamp DESC);

