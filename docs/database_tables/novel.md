# Таблица: `novel`

**Назначение:** Литературные произведения (новеллы)

## Структура

| Наименование поля | Тип данных | Обязательность | Описание / Примечание |
|---|---|---|---|
| `id` | `BIGINT` | Да | Уникальный идентификатор |
| `title` | `CHARACTER VARYING(255)` | Да |  |
| `status` | `CHARACTER VARYING(50)` | Да |  |
| `description` | `TEXT, ` | Нет |  |
| `publication_date` | `TIMESTAMP WITH` | Нет | Дата и время |
| `chapter_count` | `INTEGER` | Да |  |
| `rating_count` | `INTEGER` | Да |  |
| `author_id` | `BIGINT` | Да | Ссылка на сущность (ID) |
| `view_count` | `BIGINT` | Да |  |
| `cover_url` | `CHARACTER VARYING(512), ` | Нет |  |
| `total_score` | `BIGINT` | Да |  |
| `is_deleted` | `BOOLEAN` | Нет | Флаг (да/нет) |
| `created_at` | `TIMESTAMP WITH` | Нет | Дата и время |
| `average_rating` | `NUMERIC(3, 2)` | Да |  |
| `last_chapter_added_at` | `TIMESTAMP WITH` | Нет | Дата и время |