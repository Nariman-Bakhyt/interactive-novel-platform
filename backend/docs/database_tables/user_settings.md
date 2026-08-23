# Таблица: `user_settings`

**Назначение:** Пользовательские настройки

## Структура

| Наименование поля | Тип данных | Обязательность | Описание / Примечание |
|---|---|---|---|
| `user_id` | `BIGINT` | Да | Ссылка на сущность (ID) |
| `can_send_message` | `CHARACTER VARYING(50)` | Нет | Флаг (да/нет) |
| `library_privacy` | `CHARACTER VARYING(50)` | Нет |  |
| `communication_privacy` | `CHARACTER VARYING(50)` | Нет |  |