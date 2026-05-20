package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.dto.request.*;
import project.interactivenovelplatform.dto.response.ProfileResponseDto;
import project.interactivenovelplatform.dto.response.RelationshipStateDto;
import project.interactivenovelplatform.dto.response.UserResponseDto;
import project.interactivenovelplatform.dto.response.UserSettingsResponseDto;
import project.interactivenovelplatform.entity.*;
import project.interactivenovelplatform.error.GlobalExceptionHandler;
import project.interactivenovelplatform.repository.UserRepository;
import project.interactivenovelplatform.repository.UserSettingsRepository;
import project.interactivenovelplatform.repository.UserSpecifications;
import project.interactivenovelplatform.security.UserPrincipal;
import project.interactivenovelplatform.service.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class UserServiceImpl  implements UserService {
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final StorageService storageService;
    private final UserSettingsRepository userSettingsRepository;
    private final static Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final VerificationService verificationService;

    private final TransactionTemplate transactionTemplate;
    private final StorageHelper storageHelper;
    
    private UserResponseDto convertToDto(AppUserEntity user) {
        String publicUrl = user.getAvatarUrl() != null ? storageService.getPublicUrl(user.getAvatarUrl()) : null;
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                storageHelper.getAvatarOrDefault(publicUrl)
        );
    }


    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> searchUsers(int page , int size , String search){
        Pageable pageable = PageRequest.of(page, size);
        Specification<AppUserEntity> user = UserSpecifications.userNameLike(search);
        return userRepository.findAll(user, pageable).map(this::convertToDto);
    }
    @Override
    @Transactional(readOnly = true)
    public UserResponseDto findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username).map(this::convertToDto)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с именем: " + username + " не найден"));
    }
    @Override
    @Transactional(readOnly = true)
    public UserResponseDto findById(Long id){
        return userRepository.findById(id).map(this::convertToDto)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с id: " + id + " не найден"));
    }
    @Override
    @Transactional(readOnly = true)
    public ProfileResponseDto findProfileById(Long currentUserId ,Long targetUserId){
        ProfileResponseDto profile = userRepository.getFullProfile(targetUserId, currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id: " + targetUserId + " не найден"));

        boolean isMe = currentUserId != null && currentUserId.equals(targetUserId);
        String publicUrl = profile.getAvatarUrl() != null ? storageService.getPublicUrl(profile.getAvatarUrl()) : null;
        profile.setAvatarUrl(storageHelper.getAvatarOrDefault(publicUrl));
        if (isMe) {
            profile.setMyProfile(true);
        } else {
            profile.setMyProfile(false);
            profile.setBestFriendsCount(0);
            profile.setEmail(null);
        }
        return profile;
    }
    @Transactional(readOnly = true)
    @Override
    public RelationshipStateDto getRelationshipState(Long currentUserId, Long targetUserId){
        return userRepository.getRelationshipState(currentUserId, targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id: " + targetUserId + " не найден"));
    }
    @Transactional(readOnly = true)
    @Override
    public Map<Long, RelationshipStateDto> getRelationshipStates(Long currentUserId, List<Long> targetUserIds) {
        return userRepository.findAllRelationshipStates(currentUserId, targetUserIds)
                .stream()
                .collect(Collectors.toMap(RelationshipStateDto::getUserId, dto -> dto));
    }

    @Override
    @Transactional
    public UserResponseDto registerUser( RegistrationRequestDto dto) {

        var role = roleService.findByName("USER")
                .orElseThrow(()->new EntityNotFoundException("Такой роли: " + Role.USER + " нету"));
        Set<RoleEntity> SetRole = new HashSet<>();
        SetRole.add(role);
        var valueUser = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(dto.getUsername(), dto.getEmail()) ;
        if(valueUser.isPresent()){
            if(valueUser.get().getUsername().equalsIgnoreCase(dto.getUsername())){
                throw new DuplicateKeyException("Имя пользователя уже занято.");
            }
            if(valueUser.get().getEmail().equalsIgnoreCase(dto.getEmail())){
                throw new DuplicateKeyException("Email уже занят.");
            }
        }
        var user = new AppUserEntity(
                dto.getUsername(),
                passwordEncoder.encode(dto.getPassword()),
                dto.getEmail(),
                SetRole
        );
        var savedUser = userRepository.save(user);

        var settings = new UserSettingsEntity();
        settings.setUser(user);
        userSettingsRepository.save(settings);

        return convertToDto(savedUser);
    }

    @Override
    @Transactional
    public ProfileResponseDto updateProfileDetails(Long userId, UserUpdateRequestDto dto){
        AppUserEntity user = userRepository.findById(userId)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с таким id:" + userId + " не найден не найден"));

        boolean isUsernameChanged = dto.getNewUsername() != null && !dto.getNewUsername().equals(user.getUsername());
        boolean isEmailChanged = dto.getNewEmail() != null && !dto.getNewEmail().equals(user.getEmail());

        if (isUsernameChanged || isEmailChanged) {
            var existingUser = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                    dto.getNewUsername(),
                    dto.getNewEmail()
            );

            if (existingUser.isPresent()) {
                AppUserEntity conflictUser = existingUser.get();

                if (isUsernameChanged && conflictUser.getUsername().equalsIgnoreCase(dto.getNewUsername()) && !userId.equals(conflictUser.getId())) {
                    throw new DuplicateKeyException("Имя пользователя уже занято.");
                }

                
                if (isEmailChanged && conflictUser.getEmail().equalsIgnoreCase(dto.getNewEmail())&& !userId.equals(conflictUser.getId())) {
                    throw new DuplicateKeyException("Email уже занят.");
                }

            }
            if(isUsernameChanged){user.setUsername(dto.getNewUsername());}
            if(isEmailChanged){verificationService.sendVerificationCode(userId, VerificationTokenType.EMAIL_CHANGE, dto.getNewEmail());}
        }
        userRepository.save(user);
        return findProfileById(userId, userId) ;
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequestDto dto){
        AppUserEntity user = userRepository.findById(userId).orElseThrow(()->new EntityNotFoundException("Пользователь с таким id:" + userId + " не найден не найден"));

        if(!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())){
            throw new BadCredentialsException("Неверный текущий пароль.");
        }
        String encodedPassword = passwordEncoder.encode(dto.getNewPassword());
        user.setPasswordHash(encodedPassword);
        userRepository.save(user);
    }
    @Override
    public ProfileResponseDto uploadUserAvatar( MultipartFile file, UserPrincipal principal){
        var userId = principal.getId();
        var user = userRepository.findById(userId)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с Id: " + userId + " не найден"));
        String oldAvatarUrl = user.getAvatarUrl();

        
        String folderPath = "avatars/users/" + user.getId();
        String newAvatarUrl = null;
        try {

            if (file != null && !file.isEmpty()) {
                String actualMimeType = storageService.verifyRealImageType(file);

                String secureExtension = MimeTypes.getDefaultMimeTypes()
                        .forName(actualMimeType)
                        .getExtension();
                String filename = "avatar_" + System.currentTimeMillis() + secureExtension;
                newAvatarUrl = storageService.uploadFile(file,folderPath, filename);
            }
            String finalUrl = newAvatarUrl;
            transactionTemplate.execute(_ -> saveNewAvatarUrl(finalUrl,user.getId()));

            if (oldAvatarUrl!= null) {
                storageService.deleteFile(oldAvatarUrl);
            }
            return findProfileById(userId, userId);
        } catch (Exception e) {
            if (newAvatarUrl != null) {
                storageService.deleteFile(newAvatarUrl);
            }
            throw new RuntimeException(e);
        }

    }


    public AppUserEntity saveNewAvatarUrl(String newAvatarUrl, Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с ID: " + userId + " не найден"));
        user.setAvatarUrl(newAvatarUrl);
        return userRepository.save(user);
    }



    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        AppUserEntity user = userRepository.findByIdentifier(input)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + input));
        if (user.isLocked()) {
            throw new LockedException("Ваш аккаунт заблокирован. Пожалуйста, свяжитесь с поддержкой.");
        }

        if (!user.isActive()) {
            throw new DisabledException("Аккаунт не активирован. Проверьте вашу почту.");
        }
        return UserPrincipal.create(user);
    }

    
    private Optional<AppUserEntity> tryFindById(String input) {
        if (input != null && input.matches("\\d+")) {
            try {
                return userRepository.findById(Long.parseLong(input));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }




    public void handleFailedLogin(String username) {
        AppUserEntity user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        
        if (user.isLocked() && user.getLockTime().isBefore(OffsetDateTime.now())) {
            resetFailedAttempts(user); 
        }
        int newCount = user.getFailedAttemptCount() + 1;
        user.setFailedAttemptCount(newCount);

        if (newCount >= 3) {
            long minutesToLock = getLockDuration(newCount);

            user.setLocked(true);
            user.setLockTime(OffsetDateTime.now().plusMinutes(minutesToLock));
        }

        userRepository.save(user);
    }

    
    private long getLockDuration(int failedCount) {
        if (failedCount == 3) return 10;
        if (failedCount == 4) return 20;
        if (failedCount == 5) return 40;
        return 80; 
    }

    
    public void resetFailedAttempts(AppUserEntity user) {
        user.setLocked(false);
        user.setLockTime(null);
    }

    @Transactional
    public void handleSuccessfulLogin(String username) {
        AppUserEntity user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        
        if (user.getFailedAttemptCount() > 0 || user.isLocked()) {
            user.setFailedAttemptCount(0);
            user.setLocked(false);
            user.setLockTime(null);
            userRepository.save(user);
        }
    }

    @Transactional
    @Override
    public void forgotPassword(Long currentUserId, ResetPasswordRequestDto dto) {
        if(dto.getNewPassword() == null || dto.getNewPassword().isBlank())throw new IllegalArgumentException("не ввел новый пароль");
        verificationService.sendVerificationCode(currentUserId,VerificationTokenType.PASSWORD_RESET,passwordEncoder.encode(dto.getNewPassword()));
    }
    @Transactional
    @Override
    public void verifyCode(VerificationRequestDto dto) {
        verificationService.verifyCode(dto.getUserId(),dto.getCode() , dto.getType());
    }

    @Transactional
    @Override
    public void updateEmail(Long userId, EmailRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Пользователь с таким email уже зарегистрирован на платформе");
        }
        verificationService.sendVerificationCode(userId,VerificationTokenType.EMAIL_CHANGE, dto.getEmail());
    }

    @Transactional(readOnly = true)
    @Override
    public AppUserEntity getReference(Long id) {
        if (!userRepository.existsById(id))throw new EntityNotFoundException("Пользователь с Id:"+id+" не найден");
        return entityManager.getReference(AppUserEntity.class, id);
    }
    @Transactional(readOnly = true)
    @Override
    public AppUserEntity getReferenceByUsername(String username) {
        Long userId = userRepository.findIdByUsername(username)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с именем: " + username + " не найден"));
        return entityManager.getReference(AppUserEntity.class, userId);
    }
    @Transactional(readOnly = true)
    @Override
    public AppUserEntity getEntityIsActiveAndIsLockedFalse(Long id) {
        return userRepository.findByIdAndIsActiveTrueAndIsLockedFalse(id)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с Id: " + id + " не найден"));
    }

    @Transactional(readOnly = true)
    @Override
    public List<AppUserEntity> getAllEntitiesActiveAndNotLocked(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<AppUserEntity> users = userRepository.findAllByIdInAndIsActiveTrueAndIsLockedFalse(ids);

        if (users.size() < ids.size()) {
            log.warn("Некоторые пользователи из списка не найдены или заблокированы. Запрошено: {}, Найдено: {}",
                    ids.size(), users.size());
        }

        return users;
    }


    @Transactional(readOnly = true)
    @Override
    public AppUserEntity getEntityByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с именем: " + username + " не найден"));
    }
    @Transactional(readOnly = true)
    @Override
    public AppUserEntity getEntityByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с почтой: " + email + " не найден"));
    }

    @Transactional(readOnly = true)
    @Override
    public UserSettingsResponseDto getUserSettings(Long userId){
        var setting = userSettingsRepository.findById(userId)
                .orElseThrow(()->new EntityNotFoundException("Настройки пользователь с Id: " + userId + " не найден"));
        return new UserSettingsResponseDto(
                setting.getCanSendMessage(),
                setting.getLibraryPrivacy()
        );
    }
    @Transactional(readOnly = true)
    @Override
    public Map<Long, UserSettingsResponseDto> getUserSettingsMap(List<Long> targetUserIds) {
        return userSettingsRepository.findAllByUserIdIn(targetUserIds)
                .stream().collect(Collectors.toMap(UserSettingsEntity::getUserId,dto ->
                                new UserSettingsResponseDto(dto.getCanSendMessage(), dto.getLibraryPrivacy())));
    }

    @Transactional
    @Override
    public UserSettingsResponseDto updateUserSettings(Long userId, UserSettingsRequestDto dto){
        var setting = userSettingsRepository.findById(userId)
                .orElseThrow(()->new EntityNotFoundException("Настройки пользователь с Id: " + userId + " не найден"));

        if (dto.getCanSendMessage() != null && setting.getCanSendMessage() != dto.getCanSendMessage()) {
            setting.setCanSendMessage(dto.getCanSendMessage());
        }
        if (dto.getLibraryPrivacy() != null && setting.getLibraryPrivacy() != dto.getLibraryPrivacy()) {
            setting.setLibraryPrivacy(dto.getLibraryPrivacy());
        }
        var finalEntity = userSettingsRepository.save(setting);
        return new UserSettingsResponseDto(
                finalEntity.getCanSendMessage(),
                finalEntity.getLibraryPrivacy()
        );
    }
    @Transactional(readOnly = true)
    @Override
    public UserSettingsEntity getUserSettingsEntity(Long userId){
        return userSettingsRepository.findById(userId)
                .orElseThrow(()->new EntityNotFoundException("Настройки пользователь с Id: " + userId + " не найден"));
    }



}
