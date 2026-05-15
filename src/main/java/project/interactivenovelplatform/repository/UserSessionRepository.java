package project.interactivenovelplatform.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.UserSessionEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSessionEntity,Long> {
    Optional<UserSessionEntity> findByRefreshToken(String refreshToken);
    List<UserSessionEntity> findAllByUserIdAndIsActiveTrue(Long userId);
    @Modifying
    @Transactional
    @Query("DELETE FROM UserSessionEntity s WHERE s.expiresAt < :now")
    void deleteAllExpired(OffsetDateTime now);

    Optional<UserSessionEntity> findByUserIdAndUserAgent(Long userId, String userAgent);
}
