# Таблица: `flyway_schema_history`

**Назначение:** Служебная таблица миграций Flyway

## Структура

| Наименование поля | Тип данных | Обязательность | Описание / Примечание |
|---|---|---|---|
| `installed_rank` | `INTEGER` | Да |  |
| `version` | `CHARACTER VARYING(50), ` | Нет |  |
| `description` | `CHARACTER VARYING(200)` | Да |  |
| `type` | `CHARACTER VARYING(20)` | Да |  |
| `script` | `CHARACTER VARYING(1000)` | Да |  |
| `checksum` | `INTEGER, ` | Нет |  |
| `installed_by` | `CHARACTER VARYING(100)` | Да |  |
| `installed_on` | `TIMESTAMP WITHOUT` | Да |  |
| `execution_time` | `INTEGER` | Да |  |
| `success` | `BOOLEAN` | Да |  |