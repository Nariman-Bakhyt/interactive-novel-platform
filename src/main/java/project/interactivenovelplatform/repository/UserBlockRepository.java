package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.interactivenovelplatform.entity.UserBlockEntity;

public interface UserBlockRepository extends JpaRepository<UserBlockEntity, Long> {
}
