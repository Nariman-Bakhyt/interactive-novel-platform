package project.interactivenovelplatform.service;

import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.dto.request.*;
import project.interactivenovelplatform.dto.response.ProfileResponseDto;
import project.interactivenovelplatform.dto.response.UserResponseDto;
import project.interactivenovelplatform.dto.response.UserSettingsResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.entity.UserSettingsEntity;
import project.interactivenovelplatform.security.UserPrincipal;


public interface UserService extends UserDetailsService {
    UserResponseDto findByUsername(String username);
    UserResponseDto findById(Long id);
    ProfileResponseDto findProfileById(Long currentUserId ,Long targetUserid);

    UserResponseDto registerUser( RegistrationRequestDto dto);
    ProfileResponseDto updateProfileDetails(Long userId, UserUpdateRequestDto dto);
    void changePassword(Long userId, ChangePasswordRequestDto dto);
    ProfileResponseDto uploadUserAvatar(MultipartFile file, UserPrincipal principal);
    Page<UserResponseDto> searchUsers(int page , int size , String search);
    void handleFailedLogin(String username);
    void resetFailedAttempts(AppUserEntity user);
    void handleSuccessfulLogin(String username);

    void forgotPassword(Long currentUserId, ResetPasswordRequestDto dto);

    void verifyCode(VerificationRequestDto dto);
    void updateEmail(Long userId, EmailRequestDto dto);

    AppUserEntity getReference(Long id);
    AppUserEntity getReferenceByUsername(String username);
    AppUserEntity getEntityByUsername(String username);
    AppUserEntity getEntityByEmail(String email);
    AppUserEntity getEntityIsActiveAndIsLockedFalse(Long id);
    UserSettingsResponseDto getUserSettings(Long userId);
    UserSettingsResponseDto updateUserSettings(Long userId, UserSettingsRequestDto dto);
    UserSettingsEntity getUserSettingsEntity(Long userId);

}
