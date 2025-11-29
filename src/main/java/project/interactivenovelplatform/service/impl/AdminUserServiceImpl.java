package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.GlobalException;
import project.interactivenovelplatform.dto.request.RoleRequestDto;
import project.interactivenovelplatform.dto.request.UserNameRequestDto;
import project.interactivenovelplatform.dto.response.AdminUserResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.repository.UserRepository;
import project.interactivenovelplatform.service.AdminUserService;
import project.interactivenovelplatform.service.RoleService;

import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class AdminUserServiceImpl implements AdminUserService {
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final static Logger log = LoggerFactory.getLogger(GlobalException.class);

    private AdminUserResponseDto convertToDto(AppUserEntity user) {
        return new AdminUserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRegistrationDate(),
                user.getRole(),
                user.getIsDeleted(),
                user.getIsLocked(),
                user.getLockTime()
        );
    }

    @Override
    public Page<AdminUserResponseDto> findAll(Pageable pageable) {
        Page<AppUserEntity> entities = userRepository.findAll(pageable);
        return entities.map(this::convertToDto);
    }
    @Override
    public AdminUserResponseDto findById(Long id){
        return userRepository.findById(id).map(this::convertToDto)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с id: " + id + " не найден"));
    }
    @Override
    public AdminUserResponseDto findByUsername(UserNameRequestDto username){
        return userRepository.findByUsernameIgnoreCase(username.getUserName()).map(this::convertToDto)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с username: " + username + " не найден"));
    }

    @Override
    @Transactional
    public AdminUserResponseDto setRolesToUser(Long id, Set<RoleRequestDto> newRoleNames){
        var user = userRepository.findById(id)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с id: " + id + " не найден"));
        var user_role = roleService.findByNameIn(newRoleNames.stream().map(RoleRequestDto::getRoleName).map(String::toUpperCase).collect(Collectors.toSet()));
        if(user_role.size() != newRoleNames.size()){
            throw new EntityNotFoundException("Одна или несколько указанных ролей не существуют в базе данных.");
        }
        user.setRole(user_role);
        userRepository.save(user);
        return convertToDto(user);
    }

    @Override
    @Transactional
    public AdminUserResponseDto addRoleToUser(Long id, RoleRequestDto newRoleName){
        var user = userRepository.findById(id)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с id: " + id + " не найден"));
        var user_role = user.getRole();
        var newRole = roleService.findByName(newRoleName.getRoleName().toUpperCase())
                .orElseThrow(()->new EntityNotFoundException("Такой роли: " + newRoleName + " нету"));
        user_role.add(newRole);
        user.setRole(user_role);
        userRepository.save(user);
        return convertToDto(user);
    }
}
