package project.interactivenovelplatform.service;

import org.springframework.security.core.userdetails.UserDetailsService;
import project.interactivenovelplatform.dto.request.ChangePasswordRequestDto;
import project.interactivenovelplatform.dto.request.RegistrationRequestDto;
import project.interactivenovelplatform.dto.request.UserUpdateRequestDto;
import project.interactivenovelplatform.dto.response.UserResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;


public interface UserService extends UserDetailsService {
    UserResponseDto findByUsername(String username);
    UserResponseDto findById(Long id);
    UserResponseDto registerUser( RegistrationRequestDto dto);
    UserResponseDto updateProfileDetails(Long userId, UserUpdateRequestDto dto);
    void changePassword(Long userId, ChangePasswordRequestDto dto);
    AppUserEntity getAuthorReference(Long authorId);

    void handleFailedLogin(String username);
    void resetFailedAttempts(AppUserEntity user);
    void handleSuccessfulLogin(String username);
}
