package project.interactivenovelplatform.service;

import project.interactivenovelplatform.entity.RoleEntity;

import java.util.Optional;
import java.util.Set;

public interface RoleService {
    Set<RoleEntity> findByNameIn(Set<String> name);
    Optional<RoleEntity> findByName(String roleName);
}
