ALTER TABLE comment
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_comment_novel_active ON comment (novel_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_comment_chapter_active ON comment (chapter_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_comment_block_active ON comment (block_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_comment_forum_active ON comment (forum_topic_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_comment_channel_active ON comment (channel_post_id)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS conversations (
                                             id BIGSERIAL PRIMARY KEY,
                                             type VARCHAR(10) NOT NULL DEFAULT 'PRIVATE',
                                             title VARCHAR(255),
                                             avatar_url VARCHAR(512),
                                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                             CONSTRAINT chk_conversation_metadata CHECK (
                                                 (type = 'GROUP' AND title IS NOT NULL) OR
                                                 (type = 'PRIVATE' AND title IS NULL AND avatar_url IS NULL)
                                                 )
);

CREATE TABLE IF NOT EXISTS conversation_members (
                                                    conversation_id BIGINT REFERENCES conversations(id) ON DELETE CASCADE,
                                                    user_id BIGINT REFERENCES app_user(id) ON DELETE CASCADE,
                                                    role VARCHAR(20) DEFAULT 'MEMBER',
                                                    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                                    PRIMARY KEY (conversation_id, user_id)
);

ALTER TABLE message
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS conversation_id BIGINT REFERENCES conversations(id) ON DELETE CASCADE;

DO $$
    BEGIN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='message' AND column_name='receiver_id') THEN
            ALTER TABLE message DROP COLUMN receiver_id;
        END IF;
    END $$;


DROP TABLE IF EXISTS group_message;
DROP TABLE IF EXISTS group_member;
DROP TABLE IF EXISTS chat_group;
DROP TABLE IF EXISTS channel_member;

CREATE INDEX IF NOT EXISTS idx_message_conversation_active ON message (conversation_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_member_user ON conversation_members (user_id);