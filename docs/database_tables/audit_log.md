# Таблица: `audit_log`

**Назначение:** Журнал аудита действий (логи)

## Структура

| Наименование поля | Тип данных | Обязательность | Описание / Примечание |
|---|---|---|---|
| `id` | `BIGINT` | Да | Уникальный идентификатор |
| `action` | `CHARACTER VARYING(20)` | Да |  |
| `entity_type` | `CHARACTER VARYING(50)` | Да |  |
| `target_id` | `BIGINT` | Да | Ссылка на сущность (ID) |
| `actor_id` | `BIGINT` | Да | Ссылка на сущность (ID) |
| `reason` | `CHARACTER VARYING(100)` | Да |  |
| `details` | `TEXT, ` | Нет |  |
| `"timestamp"` | `TIMESTAMP WITH` | Нет |  |