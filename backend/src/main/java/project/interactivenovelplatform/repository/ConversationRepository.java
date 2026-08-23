package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.ConversationsEntity;
import project.interactivenovelplatform.entity.ConversationsType;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationsEntity, Long> {
    @Query("""
        SELECT c FROM ConversationsEntity c
        JOIN c.members m1 JOIN c.members m2
        WHERE c.type = :type
          AND m1.user.id = :user1Id
          AND m2.user.id = :user2Id
    """)
    Optional<ConversationsEntity> findPrivateConversationWithType(
            @Param("user1Id") Long user1Id, 
            @Param("user2Id") Long user2Id,
            @Param("type") ConversationsType type
    );

    default Optional<ConversationsEntity> findPrivateConversation(Long user1Id, Long user2Id) {
        return findPrivateConversationWithType(user1Id, user2Id, ConversationsType.PRIVATE);
    }
}
