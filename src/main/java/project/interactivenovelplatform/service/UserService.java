package project.interactivenovelplatform.service;

import org.springframework.security.core.userdetails.UserDetailsService;
import project.interactivenovelplatform.dto.request.RegistrationRequestDto;
import project.interactivenovelplatform.dto.response.UserResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;

import java.util.List;


public interface UserService extends UserDetailsService {
    List<UserResponseDto> findAll();
    UserResponseDto findByUsername(String username);
    UserResponseDto registerUser( RegistrationRequestDto dto);
    void handleFailedLogin(String username);
    void resetFailedAttempts(AppUserEntity user);
    void handleSuccessfulLogin(String username);
}
