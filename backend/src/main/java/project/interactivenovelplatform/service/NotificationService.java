package project.interactivenovelplatform.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import project.interactivenovelplatform.dto.response.NotificationResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.entity.NotificationType;

import java.util.Map;

public interface NotificationService {
    void createNotificationForFollowers(AppUserEntity author, NotificationType type, Map<String, Object> metadata, Long relatedEntityId);
    Page<NotificationResponseDto> getNotifications(Long userId, Pageable pageable);
    void markAsRead(Long userId, Long notificationId);
    void markAllAsRead(Long userId);
    void revokeChapterNotifications(Long chapterId);
    void revokeNovelNotifications(Long novelId);
    void revokeAllNotificationsByNovelId(Long novelId);
}
