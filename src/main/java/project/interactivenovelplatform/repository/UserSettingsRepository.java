package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.UserSettingsEntity;

import java.util.List;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettingsEntity, Long> {
    @Query("SELECT s FROM UserSettingsEntity s WHERE s.userId IN :userIds")
    List<UserSettingsEntity> findAllByUserIdIn(@Param("userIds") List<Long> userIds);
}
