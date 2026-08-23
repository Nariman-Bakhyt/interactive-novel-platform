package project.interactivenovelplatform.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.dto.response.NotificationResponseDto;
import project.interactivenovelplatform.dto.response.UserResponseDto;
import project.interactivenovelplatform.dto.response.WsDomain;
import project.interactivenovelplatform.dto.response.WsEventDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.entity.NotificationEntity;
import project.interactivenovelplatform.entity.NotificationType;
import project.interactivenovelplatform.error.ResourceNotFoundException;
import project.interactivenovelplatform.event.SocialWebsocketEvent;
import project.interactivenovelplatform.repository.NotificationRepository;
import project.interactivenovelplatform.service.NotificationService;
import project.interactivenovelplatform.service.UserService;
import project.interactivenovelplatform.service.UserSocialService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final UserService userService;
    private final UserSocialService userSocialService;

    @Override
    @Async
    @Transactional
    public void createNotificationForFollowers(AppUserEntity author, NotificationType type, Map<String, Object> metadata, Long relatedEntityId) {
        log.info("Starting background task to send notifications to followers of user {}", author.getId());
        
        UserResponseDto activeAuthor = userService.findById(author.getId());

        List<Long> followerIds = userSocialService.getFollowerIds(activeAuthor.getId());
        if (followerIds.isEmpty()) {
            return;
        }

        List<Long> generatedIds = notificationRepository.batchInsertNotifications(followerIds, activeAuthor.getId(), type, metadata, relatedEntityId);

        for (int i = 0; i < followerIds.size(); i++) {
            Long followerId = followerIds.get(i);
            Long notifId = (generatedIds != null && generatedIds.size() > i) ? generatedIds.get(i) : null;
            
            NotificationResponseDto payload = NotificationResponseDto.builder()
                    .id(notifId)
                    .senderId(activeAuthor.getId())
                    .senderName(activeAuthor.getUsername())
                    .senderAvatar(activeAuthor.getAvatarUrl())
                    .type(type)
                    .metadata(metadata)
                    .relatedEntityId(relatedEntityId)
                    .isRead(false)
                    .createdAt(OffsetDateTime.now())
                    .build();

            applicationEventPublisher.publishEvent(new SocialWebsocketEvent(
                    this,
                    "/topic/user." + followerId,
                    new WsEventDto<>(WsDomain.NOTIFICATION, "NOTIFICATION_RECEIVED", payload)
            ));
        }
        
        log.info("Successfully sent {} notifications", followerIds.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        int updated = notificationRepository.markAsRead(notificationId, userId);
        if (updated == 0) {
            throw new ResourceNotFoundException("Notification not found or access denied");
        }
    }
    
    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Override
    @Transactional
    public void revokeChapterNotifications(Long chapterId) {
        List<Long> recipientIds = notificationRepository.findRecipientIdsByChapterId(chapterId);
        if (recipientIds.isEmpty()) return;
        
        notificationRepository.deleteByChapterId(chapterId);
        
        for (Long recipientId : recipientIds) {
            applicationEventPublisher.publishEvent(new SocialWebsocketEvent(
                this,
                "/topic/user." + recipientId,
                new WsEventDto<>(WsDomain.NOTIFICATION, "NOTIFICATION_REVOKED", 
                    Map.of("type", "NEW_CHAPTER", "chapterId", chapterId))
            ));
        }
        log.info("Revoked {} notifications for chapter {}", recipientIds.size(), chapterId);
    }

    @Override
    @Transactional
    public void revokeNovelNotifications(Long novelId) {
        List<Long> recipientIds = notificationRepository.findRecipientIdsByNovelId(novelId);
        if (recipientIds.isEmpty()) return;
        
        notificationRepository.deleteNewNovelNotificationsByNovelId(novelId);
        
        for (Long recipientId : recipientIds) {
            applicationEventPublisher.publishEvent(new SocialWebsocketEvent(
                this,
                "/topic/user." + recipientId,
                new WsEventDto<>(WsDomain.NOTIFICATION, "NOTIFICATION_REVOKED", 
                    Map.of("type", "NEW_NOVEL", "novelId", novelId))
            ));
        }
        log.info("Revoked {} notifications for novel creation {}", recipientIds.size(), novelId);
    }

    @Override
    @Transactional
    public void revokeAllNotificationsByNovelId(Long novelId) {
        List<Long> recipientIds = notificationRepository.findAllRecipientIdsByNovelId(novelId);
        if (recipientIds.isEmpty()) return;
        
        notificationRepository.deleteAllNotificationsByNovelId(novelId);
        
        for (Long recipientId : recipientIds) {
            applicationEventPublisher.publishEvent(new SocialWebsocketEvent(
                this,
                "/topic/user." + recipientId,
                new WsEventDto<>(WsDomain.NOTIFICATION, "NOTIFICATION_REVOKED", 
                    Map.of("type", "ALL", "novelId", novelId))
            ));
        }
        log.info("Revoked {} all notifications related to novel {}", recipientIds.size(), novelId);
    }

    private NotificationResponseDto mapToDto(NotificationEntity entity) {
        return NotificationResponseDto.builder()
                .id(entity.getId())
                .senderId(entity.getSender() != null ? entity.getSender().getId() : null)
                .senderName(entity.getSender() != null ? entity.getSender().getUsername() : null)
                .senderAvatar(entity.getSender() != null ? entity.getSender().getAvatarUrl() : null)
                .type(entity.getType())
                .metadata(entity.getMetadata())
                .relatedEntityId(entity.getRelatedEntityId())
                .isRead(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
