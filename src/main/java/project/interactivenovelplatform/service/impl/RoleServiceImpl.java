package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import project.interactivenovelplatform.error.GlobalException;
import project.interactivenovelplatform.entity.Role;
import project.interactivenovelplatform.entity.RoleEntity;
import project.interactivenovelplatform.repository.RoleRepository;
import project.interactivenovelplatform.service.RoleService;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final static Logger log = LoggerFactory.getLogger(GlobalException.class);

    @Override
    public Set<RoleEntity> findByNameIn(Set<String> name){
        var setrole = roleRepository.findByNameIn(convertToRoles(name));
        if(setrole.isEmpty()){
            throw new EntityNotFoundException("Роль не найдена");
        }
        return setrole;
    }
    @Override
    public Optional<RoleEntity> findByName(String roleName){
        return roleRepository.findByName(convertToRole(roleName).orElseThrow(()->new EntityNotFoundException("Роль не найдена")));
    }

    private Set<Role> convertToRoles(Set<String> name){
        return name.stream().map(Role::valueOf).collect(Collectors.toSet());
    }
    private Optional<Role> convertToRole (String name){
        return Optional.of(Role.valueOf(name));
    }

}
