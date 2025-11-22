package project.interactivenovelplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import project.interactivenovelplatform.entity.AppUserEntity;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUserEntity, Long> {

    Optional <AppUserEntity> findByUsername(String username);

    Optional<Boolean> findByEmail(String email);
    Page<AppUserEntity> findAll(Pageable pageable);
}
