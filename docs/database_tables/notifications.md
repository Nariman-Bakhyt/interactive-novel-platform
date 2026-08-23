# Таблица: `notifications`

**Назначение:** Уведомления

## Структура

| Наименование поля | Тип данных | Обязательность | Описание / Примечание |
|---|---|---|---|
| `id` | `BIGINT` | Да | Уникальный идентификатор |
| `recipient_id` | `BIGINT` | Да | Ссылка на сущность (ID) |
| `sender_id` | `BIGINT, ` | Нет | Ссылка на сущность (ID) |
| `type` | `CHARACTER VARYING(50)` | Да |  |
| `metadata` | `JSONB` | Да |  |
| `related_entity_id` | `BIGINT, ` | Нет | Ссылка на сущность (ID) |
| `is_read` | `BOOLEAN` | Да | Флаг (да/нет) |
| `created_at` | `TIMESTAMP WITH` | Да | Дата и время |