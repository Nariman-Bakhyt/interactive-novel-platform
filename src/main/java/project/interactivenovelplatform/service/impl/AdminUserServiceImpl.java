package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.error.GlobalException;
import project.interactivenovelplatform.dto.request.RoleRequestDto;
import project.interactivenovelplatform.dto.request.UserNameRequestDto;
import project.interactivenovelplatform.dto.response.AdminUserResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.error.ResourceNotFoundException;
import project.interactivenovelplatform.error.RoleHierarchyViolationException;
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
                user.getLockTime(),
                user.getAvatarUrl()
        );
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        if (principal instanceof AppUserEntity userEntity) {
            return userEntity.getId();
        }
        throw new IllegalStateException("Principal is not of type AppUserEntity");
    }
    private int getHighestRoleRank(AppUserEntity user) {
        // Меньшее числовое значение (0, 1, 2...) соответствует более высокому рангу.
        return user.getRole().stream()
                .map(roleEntity -> roleEntity.getName().getRank())
                .min(Integer::compare)
                .orElse(Integer.MAX_VALUE);
    }

    @Override
    public Page<AdminUserResponseDto> findAll(Pageable pageable) {
        Page<AppUserEntity> entities = userRepository.findAll(pageable);
        return entities.map(this::convertToDto);
    }
    @Override
    public AdminUserResponseDto findById(Long id){
        Long callerUserId = getCurrentUserId();
        AppUserEntity caller = userRepository.findById(callerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с id: " + id + " не найден"));

        var user = userRepository.findById(id)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с id: " + id + " не найден"));
        var userRank = getHighestRoleRank(user);
        var callerRank = getHighestRoleRank(caller);
        if(userRank > callerRank) {
            return convertToDto(user);
        }
        if(id.equals(callerUserId)) {
            return convertToDto(caller);
        }
        throw new RoleHierarchyViolationException(
                "Ты не можешь смотреть аккаунт выше тебя по рангу"
        );
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
