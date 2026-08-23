# Таблица: `app_user`

**Назначение:** Пользователи платформы

## Структура

| Наименование поля | Тип данных | Обязательность | Описание / Примечание |
|---|---|---|---|
| `id` | `BIGINT` | Да | Уникальный идентификатор |
| `username` | `CHARACTER VARYING(50)` | Да |  |
| `password_hash` | `CHARACTER VARYING(255)` | Да |  |
| `email` | `CHARACTER VARYING(255)` | Да |  |
| `registration_date` | `TIMESTAMP WITH` | Нет | Дата и время |
| `is_deleted` | `BOOLEAN` | Нет | Флаг (да/нет) |
| `failed_attempt_count` | `INTEGER` | Нет |  |
| `is_locked` | `BOOLEAN` | Нет | Флаг (да/нет) |
| `lock_time` | `TIMESTAMP WITH` | Нет |  |
| `avatar_url` | `CHARACTER VARYING(512), ` | Нет |  |
| `is_active` | `BOOLEAN` | Нет | Флаг (да/нет) |