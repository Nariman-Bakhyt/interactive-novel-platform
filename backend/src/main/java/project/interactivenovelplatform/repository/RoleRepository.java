package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.Role;
import project.interactivenovelplatform.entity.RoleEntity;

import java.util.Optional;
import java.util.Set;
@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Set<RoleEntity> findByNameIn(Set<Role> names);
    Optional<RoleEntity> findByName(Role name);
}
