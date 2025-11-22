package project.interactivenovelplatform.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import project.interactivenovelplatform.dto.response.UserResponseDto;

import java.util.Set;

public interface AdminUserService extends UserDetailsService {
    UserResponseDto findById(Long id);
    Page<UserResponseDto> findAll(Pageable pageable);
    UserResponseDto setRolesToUser(Long userId, Set<String> newRoleNames);
    UserResponseDto addRoleToUser(Long userId, String newRoleName);
    //UserResponseDto toggleLockStatus(Long userId, boolean isLocked);
}
