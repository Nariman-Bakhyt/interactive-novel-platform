ALTER TABLE app_user
    DROP COLUMN role_id;
CREATE TABLE user_role
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL ,
    PRIMARY KEY (user_id,role_id),
    CONSTRAINT fk_userrole_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_userrole_role FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE RESTRICT
);

ALTER TABLE app_user
    ADD failed_attempt_count INT DEFAULT 0 ,
    ADD is_locked BOOLEAN DEFAULT FALSE,
    ADD lock_time TIMESTAMP WITH TIME ZONE ;



