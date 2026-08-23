package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.SubscribableEntity;

@Repository
public interface SubscribableEntityRepository extends JpaRepository<SubscribableEntity,Long> {
}
