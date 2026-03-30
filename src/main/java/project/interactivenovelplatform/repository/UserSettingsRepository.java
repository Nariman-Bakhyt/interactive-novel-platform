package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.interactivenovelplatform.entity.UserSettingsEntity;

public interface UserSettingsRepository extends JpaRepository<UserSettingsEntity, Long> {
}
