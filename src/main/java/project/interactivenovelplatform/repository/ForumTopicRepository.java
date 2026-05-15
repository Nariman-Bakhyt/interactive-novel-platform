package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.ForumTopicEntity;

@Repository
public interface ForumTopicRepository extends JpaRepository<ForumTopicEntity,Long> {
}
