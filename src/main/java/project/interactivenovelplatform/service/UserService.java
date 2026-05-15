package project.interactivenovelplatform.service;

import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.dto.request.*;
import project.interactivenovelplatform.dto.response.ProfileResponseDto;
import project.interactivenovelplatform.dto.response.RelationshipStateDto;
import project.interactivenovelplatform.dto.response.UserResponseDto;
import project.interactivenovelplatform.dto.response.UserSettingsResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.entity.UserSettingsEntity;
import project.interactivenovelplatform.security.UserPrincipal;

import java.util.List;
import java.util.Map;


public interface UserService extends UserDetailsService {
    UserResponseDto findByUsername(String username);
    UserResponseDto findById(Long id);
    ProfileResponseDto findProfileById(Long currentUserId ,Long targetUserid);

    RelationshipStateDto getRelationshipState(Long currentUserId, Long targetUserId);
    Map<Long, RelationshipStateDto> getRelationshipStates(Long inviterId, List<Long> targetUserIds);

    UserResponseDto registerUser(RegistrationRequestDto dto);
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
    List<AppUserEntity> getAllEntitiesActiveAndNotLocked(List<Long> ids);

    UserSettingsResponseDto getUserSettings(Long userId);
    Map<Long, UserSettingsResponseDto> getUserSettingsMap(List<Long> targetUserIds);
    UserSettingsResponseDto updateUserSettings(Long userId, UserSettingsRequestDto dto);
    UserSettingsEntity getUserSettingsEntity(Long userId);

}
