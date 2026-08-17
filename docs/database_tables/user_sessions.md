# Таблица: `user_sessions`

**Назначение:** Активные сессии пользователей

## Структура

| Наименование поля | Тип данных | Обязательность | Описание / Примечание |
|---|---|---|---|
| `id` | `BIGINT` | Да | Уникальный идентификатор |
| `user_id` | `BIGINT` | Да | Ссылка на сущность (ID) |
| `refresh_token` | `CHARACTER VARYING(255)` | Да |  |
| `user_agent` | `TEXT, ` | Нет |  |
| `ip_address` | `CHARACTER VARYING(45), ` | Нет |  |
| `login_time` | `TIMESTAMP WITHOUT` | Да |  |
| `expires_at` | `TIMESTAMP WITHOUT` | Да | Дата и время |
| `is_active` | `BOOLEAN` | Нет | Флаг (да/нет) |
| `guest_id` | `CHARACTER VARYING(255)` | Нет | Ссылка на сущность (ID) |