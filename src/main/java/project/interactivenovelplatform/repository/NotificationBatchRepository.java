package project.interactivenovelplatform.repository;

import project.interactivenovelplatform.entity.NotificationType;

import java.util.List;
import java.util.Map;

public interface NotificationBatchRepository {
    List<Long> batchInsertNotifications(List<Long> recipientIds, Long senderId, NotificationType type, Map<String, Object> metadata, Long relatedEntityId);
}
