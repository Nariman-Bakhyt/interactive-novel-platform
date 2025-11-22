package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.GlobalException;
import project.interactivenovelplatform.dto.response.UserResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.repository.UserRepository;
import project.interactivenovelplatform.service.AdminUserService;
import project.interactivenovelplatform.service.RoleService;

import java.util.Set;

@AllArgsConstructor
@Service
public class AdminUserServiceImpl implements AdminUserService {
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final static Logger log = LoggerFactory.getLogger(GlobalException.class);

    private UserResponseDto convertToDto(AppUserEntity user) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );
    }

    @Override
    public Page<UserResponseDto> findAll(Pageable pageable) {
        Page<AppUserEntity> entities = userRepository.findAll(pageable);
        return entities.map(this::convertToDto);
    }
    @Override
    public UserResponseDto findById(Long id){
        return userRepository.findById(id).map(this::convertToDto)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с id: " + id + " не найден"));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                    .orElseThrow(()->
                            new UsernameNotFoundException("User not found with username: " + username));
    }

    @Override
    @Transactional
    public UserResponseDto setRolesToUser(Long id, Set<String> newRoleNames){
        var user = userRepository.findById(id)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с id: " + id + " не найден"));;
        var user_role = roleService.findByNameIn(newRoleNames);
        if(user_role.size() != newRoleNames.size()){
            throw new EntityNotFoundException("Одна или несколько указанных ролей не существуют в базе данных.");
        }
        user.setRole(user_role);
        return convertToDto(user);
    }

    @Override
    @Transactional
    public UserResponseDto addRoleToUser(Long id, String newRoleName){
        var user = userRepository.findById(id)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с id: " + id + " не найден"));;
        var user_role = user.getRole();
        var newrole = roleService.findByName(newRoleName)
                .orElseThrow(()->new EntityNotFoundException("Такой роли: " + newRoleName + " нету"));
        user_role.add(newrole);
        user.setRole(user_role);
        return convertToDto(user);
    }
}
