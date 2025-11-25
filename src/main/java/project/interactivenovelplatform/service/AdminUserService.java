package project.interactivenovelplatform.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import project.interactivenovelplatform.dto.request.RoleRequestDto;
import project.interactivenovelplatform.dto.request.UserNameRequestDto;
import project.interactivenovelplatform.dto.response.AdminUserResponseDto;

import java.util.Set;

public interface AdminUserService  {
    AdminUserResponseDto findById(Long id);
    Page<AdminUserResponseDto> findAll(Pageable pageable);
    AdminUserResponseDto findByUsername(UserNameRequestDto username);
    AdminUserResponseDto setRolesToUser(Long userId, Set<RoleRequestDto> newRoleNames);
    AdminUserResponseDto addRoleToUser(Long userId, RoleRequestDto newRoleName);
    //AdminUserResponseDto toggleLockStatus(Long userId, boolean isLocked);
}
