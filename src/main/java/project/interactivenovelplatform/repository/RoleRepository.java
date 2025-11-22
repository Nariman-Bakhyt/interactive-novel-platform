package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.interactivenovelplatform.entity.RoleEntity;

import java.util.Optional;
import java.util.Set;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Set<RoleEntity> findByNameIn(Set<String> names);
    Optional<RoleEntity> findByName(String name);
}
