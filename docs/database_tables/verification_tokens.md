# Таблица: `verification_tokens`

**Назначение:** Токены верификации (email и др.)

## Структура

| Наименование поля | Тип данных | Обязательность | Описание / Примечание |
|---|---|---|---|
| `id` | `BIGINT` | Да | Уникальный идентификатор |
| `token` | `CHARACTER VARYING(64)` | Да |  |
| `user_id` | `BIGINT` | Да | Ссылка на сущность (ID) |
| `type` | `CHARACTER VARYING(50)` | Да |  |
| `expiry_date` | `TIMESTAMP WITHOUT` | Да | Дата и время |
| `pending_value` | `CHARACTER VARYING(555)` | Нет |  |