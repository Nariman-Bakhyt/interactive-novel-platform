package project.interactivenovelplatform.dto;

public enum SocialEventType {
    // Подписки
    FOLLOW_SUCCESS ,       // Мы или на нас подписались
    UNFOLLOW_SUCCESS ,   // Отписка
    // Друзья
    FRIEND_REQUEST_SENT ,         // Исходящая заявка
    FRIEND_REQUEST_RECEIVED , // Входящая заявка
    FRIEND_REQUEST_ACCEPTED , // Заявка принята (стали друзьями)
    FRIEND_REQUEST_DECLINED , // Отказ или отмена заявки
    // Близкие друзья
    CLOSE_FRIEND_ADDED ,     // Добавлен в звезды
    CLOSE_FRIEND_REMOVED , // Удален из звезд
    // Блокировка
    USER_BLOCKED ,     // Попал в ЧС
    USER_UNBLOCKED  // Вышел из ЧС
}
