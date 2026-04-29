DO $$
    BEGIN
        IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'app_user' and column_name = 'activation_code') then
            ALTER TABLE IF EXISTS app_user
                DROP activation_code ;
        end if;

END$$;

CREATE TABLE IF NOT EXISTS verification_tokens(
    id  BIGSERIAL PRIMARY KEY ,
    token VARCHAR(64) NOT NULL ,
    user_id BIGINT NOT NULL ,
    type VARCHAR(50) NOT NULL ,
    expiry_date timestamp NOT NULL ,
    CONSTRAINT uk_verification_type UNIQUE (user_id,type),
    CONSTRAINT fk_token_user_id FOREIGN KEY (user_id)
                                REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_verification_token_full_lookup ON verification_tokens(token, type, user_id);

CREATE INDEX IF NOT EXISTS idx_verification_token_expiry ON verification_tokens(expiry_date);