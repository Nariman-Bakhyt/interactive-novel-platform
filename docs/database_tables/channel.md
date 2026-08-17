# Таблица: `channel`

**Назначение:** Пользовательские каналы

## Структура

| Наименование поля | Тип данных | Обязательность | Описание / Примечание |
|---|---|---|---|
| `id` | `BIGINT` | Да | Уникальный идентификатор |
| `name` | `CHARACTER VARYING(255)` | Да |  |
| `description` | `TEXT, ` | Нет |  |
| `avatar_url` | `CHARACTER VARYING(500), ` | Нет |  |
| `is_private` | `BOOLEAN` | Нет | Флаг (да/нет) |
| `author_id` | `BIGINT` | Да | Ссылка на сущность (ID) |
| `created_at` | `TIMESTAMP WITH` | Нет | Дата и время |