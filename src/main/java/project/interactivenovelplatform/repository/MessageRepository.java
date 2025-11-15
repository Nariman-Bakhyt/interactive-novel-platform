package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.interactivenovelplatform.Entity.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
}
