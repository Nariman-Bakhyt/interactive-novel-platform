-- Обновление таблицы пользователя: AppUserEntity
ALTER TABLE app_user
    ADD COLUMN avatar_url VARCHAR(512);

-- Обновление таблицы новеллы: Novel (или NovelEntity)
ALTER TABLE novel
    ADD COLUMN cover_url VARCHAR(512);