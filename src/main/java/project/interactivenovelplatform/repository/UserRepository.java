package project.interactivenovelplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import project.interactivenovelplatform.entity.AppUserEntity;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUserEntity, Long> {

    Optional <AppUserEntity> findByUsernameIgnoreCase(String username);
    Optional<Boolean> findByEmailIgnoreCase(String email);
    Page<AppUserEntity> findAll(Pageable pageable);
    @Query("SELECT u FROM AppUserEntity u WHERE UPPER(u.username)= UPPER(:username) OR UPPER(u.email)= UPPER(:email) ")
    Optional<AppUserEntity> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);
}
