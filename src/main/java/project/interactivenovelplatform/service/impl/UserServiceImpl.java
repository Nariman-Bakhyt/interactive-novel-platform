package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import project.interactivenovelplatform.GlobalException;
import project.interactivenovelplatform.dto.request.RegistrationRequestDto;
import project.interactivenovelplatform.dto.response.UserResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.entity.Role;
import project.interactivenovelplatform.entity.RoleEntity;
import project.interactivenovelplatform.repository.RoleRepository;
import project.interactivenovelplatform.repository.UserRepository;
import project.interactivenovelplatform.service.UserService;

import java.time.ZonedDateTime;
import java.util.*;

@Service
public class UserServiceImpl  implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final static Logger log = LoggerFactory.getLogger(GlobalException.class);
    private final PasswordEncoder passwordEncoder;
    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }
    @Override
    public List<UserResponseDto> findAll() {
        List<AppUserEntity> entities = userRepository.findAll();

        List<UserResponseDto> dtoList = new ArrayList<>();

        for (AppUserEntity entity : entities) {
            UserResponseDto dto = new UserResponseDto();

            dto.setId(entity.getId());
            dto.setUsername(entity.getUsername());
            dto.setEmail(entity.getEmail());

            dtoList.add(dto);
        }
        return dtoList;
    }

    @Override
    public UserResponseDto findByUsername(String username) {
        return userRepository.findByUsername(username).map(user->
                new UserResponseDto(
                        user.getId(),
                        username,
                        user.getEmail()
                )
        ).orElseThrow(()->new EntityNotFoundException("User not found with username: " + username));
    }

    @Override
    public UserResponseDto registerUser( RegistrationRequestDto dto) {

        var role = roleRepository.findByName(Role.USER).orElseThrow(()-> new EntityNotFoundException("Нету Ролей"));
        Set<RoleEntity> SetRole = new HashSet<>();
        SetRole.add(role);
        var user = new AppUserEntity(
                dto.getUsername(),
                passwordEncoder.encode(dto.getPassword()),
                dto.getEmail(),
                SetRole
        );
        var savedUser = userRepository.save(user);
        return new UserResponseDto(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail()
        );
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(()->
                        new UsernameNotFoundException("User not found with username: " + username));
    }


    public void handleFailedLogin(String username) {
        AppUserEntity user = userRepository.findByUsername(username)
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
        AppUserEntity user = userRepository.findByUsername(username)
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
