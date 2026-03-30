package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.interactivenovelplatform.entity.UserRelationEntity;

public interface UserRelationRepository extends JpaRepository<UserRelationEntity, Long> {
}
