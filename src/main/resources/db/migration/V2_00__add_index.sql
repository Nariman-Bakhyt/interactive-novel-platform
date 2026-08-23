-- Индекс на внешний ключ (для ускорения JOIN)
CREATE INDEX IF NOT EXISTS idx_app_user_role_id
    ON app_user (role_id);

-- Индекс на дату регистрации (для ускорения сортировки и временных запросов)
CREATE INDEX IF NOT EXISTS idx_app_user_registration_date
    ON app_user (registration_date);