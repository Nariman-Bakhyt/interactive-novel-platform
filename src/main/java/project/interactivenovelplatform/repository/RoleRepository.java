package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.interactivenovelplatform.entity.Role;
import project.interactivenovelplatform.entity.RoleEntity;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByName(Role name);
}
