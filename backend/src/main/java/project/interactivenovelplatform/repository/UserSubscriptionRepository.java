package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.SubscribableType;
import project.interactivenovelplatform.entity.UserSubscriptionEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscriptionEntity, Long> {
    
    Optional<UserSubscriptionEntity> findByUserIdAndTargetIdAndIsDeletedFalse(Long userId, Long targetId);

    
    List<UserSubscriptionEntity> findAllByUserIdAndTargetTypeAndIsDeletedFalse(Long userId, SubscribableType targetType);

    
    boolean existsByUserIdAndTargetIdAndIsDeletedFalse(Long userId, Long targetId);
}
