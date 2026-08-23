alter table app_user
add if not exists is_active boolean default false ,
add if not exists activation_code text;

CREATE TABLE if not exists user_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    refresh_token VARCHAR(255) NOT NULL UNIQUE,
    user_agent TEXT,
    ip_address VARCHAR(45),
    login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,


    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX if not exists idx_sessions_expires_at ON user_sessions(expires_at);

