package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.entity.VerificationTokenEntity;
import project.interactivenovelplatform.entity.VerificationTokenType;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface VerificationRepository extends JpaRepository<VerificationTokenEntity,Long> {
    Optional<VerificationTokenEntity> findByTokenAndTypeAndUser_id(String token, VerificationTokenType type, Long userId);

    
    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationTokenEntity v WHERE v.user.id = :userId AND v.type = :type")
    void deleteByUserIdAndType(Long userId, VerificationTokenType type);

    
    @Modifying
    @Transactional
    void deleteAllByExpiryDateBefore(OffsetDateTime now);

}
