# Таблица: `chapter`

**Назначение:** Главы новелл

## Структура

| Наименование поля | Тип данных | Обязательность | Описание / Примечание |
|---|---|---|---|
| `id` | `BIGINT` | Да | Уникальный идентификатор |
| `novel_id` | `BIGINT` | Да | Ссылка на сущность (ID) |
| `chapter_number` | `DOUBLE PRECISION` | Да |  |
| `title` | `CHARACTER VARYING(255)` | Да |  |
| `is_deleted` | `BOOLEAN` | Нет | Флаг (да/нет) |
| `created_at` | `TIMESTAMP WITH` | Да | Дата и время |
| `published_at` | `TIMESTAMP WITH` | Нет | Дата и время |
| `status` | `CHARACTER VARYING(20)` | Да |  |