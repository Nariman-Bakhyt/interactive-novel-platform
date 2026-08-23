# Таблица: `comment`

**Назначение:** Комментарии пользователей

## Структура

| Наименование поля | Тип данных | Обязательность | Описание / Примечание |
|---|---|---|---|
| `id` | `BIGINT` | Да | Уникальный идентификатор |
| `user_id` | `BIGINT` | Да | Ссылка на сущность (ID) |
| `content` | `TEXT` | Да |  |
| `"timestamp"` | `TIMESTAMP WITH` | Нет |  |
| `parent_comment_id` | `BIGINT, ` | Нет | Ссылка на сущность (ID) |
| `novel_id` | `BIGINT, ` | Нет | Ссылка на сущность (ID) |
| `chapter_id` | `BIGINT, ` | Нет | Ссылка на сущность (ID) |
| `forum_topic_id` | `BIGINT, ` | Нет | Ссылка на сущность (ID) |
| `channel_post_id` | `BIGINT, ` | Нет | Ссылка на сущность (ID) |
| `block_id` | `BIGINT, ` | Нет | Ссылка на сущность (ID) |
| `is_deleted` | `BOOLEAN` | Нет | Флаг (да/нет) |
| `metadata` | `JSONB` | Нет |  |