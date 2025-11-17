package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.interactivenovelplatform.entity.AppUserEntity;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUserEntity, Long> {

    Optional <AppUserEntity> findByUsername(String username);
}
