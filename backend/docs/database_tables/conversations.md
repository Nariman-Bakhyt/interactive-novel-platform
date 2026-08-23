# Таблица: `conversations`

**Назначение:** Беседы (диалоги и группы)

## Структура

| Наименование поля | Тип данных | Обязательность | Описание / Примечание |
|---|---|---|---|
| `id` | `BIGINT` | Да | Уникальный идентификатор |
| `type` | `CHARACTER VARYING(10)` | Да |  |
| `title` | `CHARACTER VARYING(255), ` | Нет |  |
| `avatar_url` | `CHARACTER VARYING(512), ` | Нет |  |
| `created_at` | `TIMESTAMP WITH` | Нет | Дата и время |
| `last_message_at` | `TIMESTAMP WITH` | Нет | Дата и время |
| `last_message_preview` | `CHARACTER VARYING(255), ` | Нет |  |
| `is_deleted` | `BOOLEAN` | Нет | Флаг (да/нет) |