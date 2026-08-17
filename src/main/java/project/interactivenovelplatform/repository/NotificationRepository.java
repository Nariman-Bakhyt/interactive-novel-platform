package project.interactivenovelplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.NotificationEntity;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long>, NotificationBatchRepository {
    
    @EntityGraph(attributePaths = {"sender"})
    Page<NotificationEntity> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.recipient.id = :recipientId AND n.isRead = false")
    void markAllAsRead(@org.springframework.data.repository.query.Param("recipientId") Long recipientId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.id = :notificationId AND n.recipient.id = :recipientId")
    int markAsRead(@org.springframework.data.repository.query.Param("notificationId") Long notificationId, @org.springframework.data.repository.query.Param("recipientId") Long recipientId);

    // For chapters
    @org.springframework.data.jpa.repository.Query(value = "SELECT recipient_id FROM notifications WHERE type = 'NEW_CHAPTER' AND CAST(metadata ->> 'chapterId' AS bigint) = :chapterId", nativeQuery = true)
    java.util.List<Long> findRecipientIdsByChapterId(@org.springframework.data.repository.query.Param("chapterId") Long chapterId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM notifications WHERE type = 'NEW_CHAPTER' AND CAST(metadata ->> 'chapterId' AS bigint) = :chapterId", nativeQuery = true)
    void deleteByChapterId(@org.springframework.data.repository.query.Param("chapterId") Long chapterId);

    // For novel hiding (only NEW_NOVEL)
    @org.springframework.data.jpa.repository.Query(value = "SELECT recipient_id FROM notifications WHERE type = 'NEW_NOVEL' AND CAST(metadata ->> 'novelId' AS bigint) = :novelId", nativeQuery = true)
    java.util.List<Long> findRecipientIdsByNovelId(@org.springframework.data.repository.query.Param("novelId") Long novelId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM notifications WHERE type = 'NEW_NOVEL' AND CAST(metadata ->> 'novelId' AS bigint) = :novelId", nativeQuery = true)
    void deleteNewNovelNotificationsByNovelId(@org.springframework.data.repository.query.Param("novelId") Long novelId);

    // For novel deletion (all notifications related to novel)
    @org.springframework.data.jpa.repository.Query(value = "SELECT recipient_id FROM notifications WHERE CAST(metadata ->> 'novelId' AS bigint) = :novelId", nativeQuery = true)
    java.util.List<Long> findAllRecipientIdsByNovelId(@org.springframework.data.repository.query.Param("novelId") Long novelId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM notifications WHERE CAST(metadata ->> 'novelId' AS bigint) = :novelId", nativeQuery = true)
    void deleteAllNotificationsByNovelId(@org.springframework.data.repository.query.Param("novelId") Long novelId);
}
