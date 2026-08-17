# Таблица: `user_library`

**Назначение:** Библиотека пользователя

## Структура

| Наименование поля | Тип данных | Обязательность | Описание / Примечание |
|---|---|---|---|
| `user_id` | `BIGINT` | Да | Ссылка на сущность (ID) |
| `novel_id` | `BIGINT` | Да | Ссылка на сущность (ID) |
| `status` | `CHARACTER VARYING(50)` | Да |  |
| `privacy_level` | `CHARACTER VARYING(50)` | Нет |  |
| `created_at` | `TIMESTAMP WITH` | Да | Дата и время |