package project.interactivenovelplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.interactivenovelplatform.entity.ConversationMembersEntity;
import project.interactivenovelplatform.entity.ConversationMembersId;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ConversationMemberRepository extends JpaRepository<ConversationMembersEntity, ConversationMembersId> {
    // Получить список чатов пользователя (отсортированных по последнему сообщению)
    @Query(value = """
        SELECT cm FROM ConversationMembersEntity cm 
        JOIN FETCH cm.conversation c 
        WHERE cm.user.id = :userId AND cm.isDeleted = false 
        ORDER BY cm.isPinned DESC, c.lastMessageAt DESC
    """,
    countQuery = """
        SELECT COUNT(cm) FROM ConversationMembersEntity cm 
        WHERE cm.user.id = :userId AND cm.isDeleted = false
    """)
    Page<ConversationMembersEntity> findActiveChatsForUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT m FROM ConversationMembersEntity m " +
            "WHERE m.id = :chatId AND m.user.id = :userId")
    Optional<ConversationMembersEntity> findEntityByIdAndUserId(@Param("chatId") ConversationMembersId chatId, @Param("userId") Long userId);

    @EntityGraph(attributePaths = {"user", "conversation"})
    Optional<ConversationMembersEntity> findByConversationIdAndUserId(Long conversationId, Long userId);

    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    @Modifying
    @Query("UPDATE ConversationMembersEntity m SET m.isDeleted = false " +
            "WHERE m.conversation.id = :conversationId AND m.isDeleted = true")
    void restoreAllMembersInConversation(@Param("conversationId") Long conversationId);

    // Метод для быстрого получения всех ID участников (для рассылки WebSocket)
    @Query("SELECT m.user.id FROM ConversationMembersEntity m WHERE m.conversation.id = :conversationId")
    List<Long> findAllMemberIdsByConversationId(@Param("conversationId") Long conversationId);

    @Query("SELECT m.user.id FROM ConversationMembersEntity m " +
            "WHERE m.conversation.id = :conversationId AND m.user.id IN :userIds")
    List<Long> findAllUserIdsInConversation(Long conversationId, List<Long> userIds);

}
