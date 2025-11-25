package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import project.interactivenovelplatform.GlobalException;
import project.interactivenovelplatform.dto.request.ChangePasswordRequestDto;
import project.interactivenovelplatform.dto.request.RegistrationRequestDto;
import project.interactivenovelplatform.dto.request.UserUpdateRequestDto;
import project.interactivenovelplatform.dto.response.UserResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.entity.Role;
import project.interactivenovelplatform.entity.RoleEntity;
import project.interactivenovelplatform.repository.UserRepository;
import project.interactivenovelplatform.service.RoleService;
import project.interactivenovelplatform.service.UserService;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@Service
public class UserServiceImpl  implements UserService {
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final static Logger log = LoggerFactory.getLogger(GlobalException.class);
    private final PasswordEncoder passwordEncoder;

    private UserResponseDto convertToDto(AppUserEntity user) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );
    }

    @Override
    public UserResponseDto findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username).map(this::convertToDto)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с именем: " + username + " не найден"));
    }
    @Override
    public UserResponseDto findById(Long id){
        return userRepository.findById(id).map(this::convertToDto)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с id: " + id + " не найден"));
    }
    @Override
    public AppUserEntity getAuthorReference(Long authorId){
        return userRepository.findById(authorId).orElseThrow(()->new EntityNotFoundException("Пользователь с id: " + authorId + " не найден"));
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
        return convertToDto(savedUser);
    }

    @Override
    @Transactional
    public UserResponseDto updateProfileDetails(Long userId, UserUpdateRequestDto dto){
        AppUserEntity user = userRepository.findById(userId)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с таким id:" + userId + " не найден не найден"));

        // Проверка, изменились ли поля
        boolean isUsernameChanged = dto.getNewUsername() != null && !dto.getNewUsername().equals(user.getUsername());
        boolean isEmailChanged = dto.getNewEmail() != null && !dto.getNewEmail().equals(user.getEmail());

        if (isUsernameChanged || isEmailChanged) {

            // 1. Выполняем ОДИН запрос для проверки обоих полей
            var existingUser = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                    dto.getNewUsername(),
                    dto.getNewEmail()
            );

            if (existingUser.isPresent()) {
                AppUserEntity conflictUser = existingUser.get();

                // Конфликт по ИМЕНИ (проверяем, что введенное имя совпадает с именем конфликтующего пользователя)
                if (isUsernameChanged && conflictUser.getUsername().equalsIgnoreCase(dto.getNewUsername()) && !userId.equals(conflictUser.getId())) {
                    throw new DuplicateKeyException("Имя пользователя уже занято.");
                }

                // Конфликт по EMAIL
                if (isEmailChanged && conflictUser.getEmail().equalsIgnoreCase(dto.getNewEmail())&& !userId.equals(conflictUser.getId())) {
                    throw new DuplicateKeyException("Email уже занят.");
                }

            }
            if(isUsernameChanged){user.setUsername(dto.getNewUsername());}
            if(isEmailChanged){user.setEmail(dto.getNewEmail());}
        }
        userRepository.save(user);
        return convertToDto(user) ;
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
    }



    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(()->
                        new UsernameNotFoundException("User not found with username: " + username));
    }


    public void handleFailedLogin(String username) {
        AppUserEntity user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        // Снимаем блокировку, если время истекло, но флаг isLocked не сброшен (на всякий случай)
        if (user.getIsLocked() != null && user.getIsLocked() && user.getLockTime().isBefore(ZonedDateTime.now())) {
            resetFailedAttempts(user); // Сбрасываем счетчик и флаги
        }
        int newCount = user.getFailedAttemptCount() + 1;
        user.setFailedAttemptCount(newCount);

        if (newCount >= 3) {
            long minutesToLock = getLockDuration(newCount);

            user.setIsLocked(true);
            user.setLockTime(ZonedDateTime.now().plusMinutes(minutesToLock));
        }

        userRepository.save(user);
    }

    // Вспомогательный метод для определения времени блокировки
    private long getLockDuration(int failedCount) {
        if (failedCount == 3) return 10;
        if (failedCount == 4) return 20;
        if (failedCount == 5) return 40;
        return 80; // 80 минут для всех последующих
    }

    // Метод для сброса счетчиков после успешного входа или истечения времени
    public void resetFailedAttempts(AppUserEntity user) {
        user.setIsLocked(false);
        user.setLockTime(null);
    }

    @Transactional
    public void handleSuccessfulLogin(String username) {
        AppUserEntity user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        // Если счетчик равен 0 и isLocked = false, ничего не делаем.
        if (user.getFailedAttemptCount() > 0 || user.getIsLocked()) {
            user.setFailedAttemptCount(0);
            user.setIsLocked(false);
            user.setLockTime(null);
            userRepository.save(user);
        }
    }
}
