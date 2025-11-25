-- Индекс для быстрого поиска по имени (нечувствительно к регистру)
CREATE INDEX idx_app_user_username_upper ON app_user (UPPER(username));

-- Индекс для быстрого поиска по email (нечувствительно к регистру)
CREATE INDEX idx_app_user_email_upper ON app_user (UPPER(email));

