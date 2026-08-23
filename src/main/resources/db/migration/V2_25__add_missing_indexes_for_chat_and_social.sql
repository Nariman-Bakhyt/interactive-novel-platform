-- Индексы для Chat (чат)

-- Оптимизация поиска сообщений в беседе (пейджинг, сортировка по времени)
CREATE INDEX IF NOT EXISTS idx_message_conversation_timestamp ON message (conversation_id, timestamp DESC) WHERE is_deleted = false;

-- Оптимизация проверки принадлежности пользователя к беседе
CREATE INDEX IF NOT EXISTS idx_conv_members_pair ON conversation_members (conversation_id, user_id);

-- Индексы для UserSocial (социальные функции)

-- Оптимизация блокировок (кто кого заблокировал)
CREATE INDEX IF NOT EXISTS idx_user_blocks_pair ON user_blocks (blocker_id, blocked_id);
CREATE INDEX IF NOT EXISTS idx_user_blocks_blocked ON user_blocks (blocked_id);

-- Оптимизация поиска дружеских связей (в обе стороны)
CREATE INDEX IF NOT EXISTS idx_user_friends_pair ON user_friends (sender_id, receiver_id);
-- Оптимизация запросов друзей по статусу (например, поиск PENDING заявок)
CREATE INDEX IF NOT EXISTS idx_user_friends_receiver_status ON user_friends (receiver_id, status);
CREATE INDEX IF NOT EXISTS idx_user_friends_sender_status ON user_friends (sender_id, status);

-- Оптимизация подписок (фолловеров)
CREATE INDEX IF NOT EXISTS idx_user_followers_pair ON user_followers (sender_id, receiver_id);

-- Оптимизация близких друзей
CREATE INDEX IF NOT EXISTS idx_user_close_friends_pair ON user_close_friends (owner_id, friend_id);

-- Оптимизация кастомных подписок
CREATE INDEX IF NOT EXISTS idx_user_subscriptions_pair ON user_subscriptions (user_id, target_id) WHERE is_deleted = false;
