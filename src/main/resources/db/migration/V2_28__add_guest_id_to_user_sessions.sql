ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS guest_id VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id_guest ON user_sessions (user_id, guest_id);
