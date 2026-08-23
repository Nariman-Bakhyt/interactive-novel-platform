DO $$
    BEGIN
        IF EXISTS (SELECT 1 FROM  information_schema.columns where table_name = 'user_settings' and  column_name = 'show_library') then
            ALTER TABLE IF EXISTS user_settings
                ALTER COLUMN show_library TYPE VARCHAR(50)
                    USING CASE
                              WHEN show_library = true THEN 'EVERYONE'
                              ELSE 'NOBODY'
                    END,
                ALTER COLUMN show_library SET DEFAULT 'NOBODY';

            ALTER TABLE IF EXISTS user_settings RENAME COLUMN show_library TO library_privacy;
        END IF;

        IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'user_library' and  column_name = 'is_private') then
            ALTER TABLE IF EXISTS user_library
                ALTER COLUMN is_private TYPE VARCHAR(50)
                    USING CASE
                              WHEN is_private = true THEN 'NOBODY'
                              ELSE 'EVERYONE'
                    END,
                ALTER COLUMN is_private SET DEFAULT 'NOBODY';

            ALTER TABLE IF EXISTS user_library RENAME COLUMN is_private TO privacy_level;
        END IF;

END $$;




CREATE TABLE IF NOT EXISTS user_close_friends (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    friend_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    added_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Ограничение: нельзя добавить одного и того же друга дважды в свой список
    CONSTRAINT uq_owner_friend UNIQUE (owner_id, friend_id)
);

CREATE INDEX IF NOT EXISTS idx_close_friends_friend_id ON user_close_friends (friend_id);