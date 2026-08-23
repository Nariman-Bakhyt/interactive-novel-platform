# Таблица: `user_subscriptions`

**Назначение:** Подписки пользователей

## Структура

| Наименование поля | Тип данных | Обязательность | Описание / Примечание |
|---|---|---|---|
| `id` | `BIGINT` | Да | Уникальный идентификатор |
| `user_id` | `BIGINT` | Да | Ссылка на сущность (ID) |
| `target_id` | `BIGINT` | Да | Ссылка на сущность (ID) |
| `target_type` | `CHARACTER VARYING(50)` | Да |  |
| `is_pinned` | `BOOLEAN` | Нет | Флаг (да/нет) |
| `is_muted` | `BOOLEAN` | Нет | Флаг (да/нет) |
| `is_deleted` | `BOOLEAN` | Нет | Флаг (да/нет) |
| `last_read_at` | `TIMESTAMP WITH` | Нет | Дата и время |
| `created_at` | `TIMESTAMP WITH` | Нет | Дата и время |
| `role` | `CHARACTER VARYING(50)` | Нет |  |