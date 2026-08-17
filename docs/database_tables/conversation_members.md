# Таблица: `conversation_members`

**Назначение:** Участники бесед

## Структура

| Наименование поля | Тип данных | Обязательность | Описание / Примечание |
|---|---|---|---|
| `conversation_id` | `BIGINT` | Да | Ссылка на сущность (ID) |
| `user_id` | `BIGINT` | Да | Ссылка на сущность (ID) |
| `role` | `CHARACTER VARYING(20)` | Нет |  |
| `joined_at` | `TIMESTAMP WITH` | Нет | Дата и время |
| `is_pinned` | `BOOLEAN` | Нет | Флаг (да/нет) |
| `is_muted` | `BOOLEAN` | Нет | Флаг (да/нет) |
| `last_read_at` | `TIMESTAMP WITH` | Нет | Дата и время |
| `is_deleted` | `BOOLEAN` | Нет | Флаг (да/нет) |
| `cleared_at` | `TIMESTAMP WITH` | Нет | Дата и время |