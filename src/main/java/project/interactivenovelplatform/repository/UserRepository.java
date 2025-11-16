package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.interactivenovelplatform.entity.AppUserEntity;

import java.util.List;

public interface UserRepository extends JpaRepository<AppUserEntity, Long> {

    AppUserEntity findByUsername(String username);
}
