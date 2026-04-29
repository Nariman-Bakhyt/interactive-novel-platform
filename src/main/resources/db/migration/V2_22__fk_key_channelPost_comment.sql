ALTER TABLE IF EXISTS comment
    ADD CONSTRAINT fk_comment_forum
        FOREIGN KEY (forum_topic_id) REFERENCES forum_topic(id) ON DELETE CASCADE;
ALTER TABLE IF EXISTS channel_post
    ADD CONSTRAINT fk_channel_post_channelId
        FOREIGN KEY (channel_id) REFERENCES channel(id) ON DELETE CASCADE ;
ALTER TABLE IF EXISTS user_subscriptions
    ADD COLUMN role VARCHAR(50) DEFAULT 'SUBSCRIBER';

CREATE INDEX IF NOT EXISTS idx_user_sub_role
    ON user_subscriptions(user_id, target_id, role);