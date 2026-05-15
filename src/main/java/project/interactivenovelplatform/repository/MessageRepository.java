package project.interactivenovelplatform.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.MessageEntity;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    @EntityGraph(attributePaths = {"sender"})
    Optional<MessageEntity> findById(Long id);

    @Query("""
    SELECT m FROM MessageEntity m 
    WHERE m.conversation.id = :conversationId 
      AND m.isDeleted = false 
      AND (cast(:clearedAt as timestamp) IS NULL OR m.timestamp > :clearedAt)
    """)
    Page<MessageEntity> findMessages(
            @Param("conversationId") Long conversationId,
            @Param("clearedAt") OffsetDateTime clearedAt,
            Pageable pageable
    );
}
