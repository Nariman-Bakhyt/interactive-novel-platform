package project.interactivenovelplatform.service;

import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.dto.request.ChangePasswordRequestDto;
import project.interactivenovelplatform.dto.request.RegistrationRequestDto;
import project.interactivenovelplatform.dto.request.UserUpdateRequestDto;
import project.interactivenovelplatform.dto.response.NovelResponseDto;
import project.interactivenovelplatform.dto.response.UserResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;

import java.security.Principal;


public interface UserService extends UserDetailsService {
    UserResponseDto findByUsername(String username);
    UserResponseDto findById(Long id);
    UserResponseDto registerUser( RegistrationRequestDto dto);
    UserResponseDto updateProfileDetails(Long userId, UserUpdateRequestDto dto);
    void changePassword(Long userId, ChangePasswordRequestDto dto);
    AppUserEntity getAuthorReference(Long authorId);
    UserResponseDto uploadUserAvatar(@RequestParam("file") MultipartFile file, Principal principal);
    Page<UserResponseDto> searchUsers(int page , int size , String search);
    void handleFailedLogin(String username);
    void resetFailedAttempts(AppUserEntity user);
    void handleSuccessfulLogin(String username);
}
