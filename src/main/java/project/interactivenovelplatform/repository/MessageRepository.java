package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.interactivenovelplatform.entity.MessageEntity;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
}
