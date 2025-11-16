package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl  implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final static Logger log = LoggerFactory.getLogger(GlobalException.class);

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }
    @Override
    public List<UserResponseDto> findAll() {
        // 1. Получаем список сущностей
        List<AppUserEntity> entities = userRepository.findAll();

        // 2. Создаем пустой список для DTO
        List<UserResponseDto> dtoList = new ArrayList<>();

        // 3. Преобразуем каждую сущность в DTO
        for (AppUserEntity entity : entities) {
            UserResponseDto dto = new UserResponseDto();

            // Маппинг полей вручную
            dto.setId(entity.getId());
            dto.setUsername(entity.getUsername());
            dto.setEmail(entity.getEmail());

            // Важно: не включаем пароль!

            dtoList.add(dto);
        }

        // 4. Возвращаем список DTO
        return dtoList;
    }

    @Override
    public UserResponseDto findByUsername(String username) {
        var user = userRepository.findByUsername(username);
        if (user == null) {
            throw new EntityNotFoundException("User not found with username: " + username);
        }
        UserResponseDto dto = new UserResponseDto(
                user.getId(),
                username,
                user.getEmail()
        );
        return dto;
    }

    @Override
    public UserResponseDto registerUser( RegistrationRequestDto dto) {

        Optional<RoleEntity> role = roleRepository.findByName(Role.USER);
        if (role.isEmpty()){
            throw new EntityNotFoundException("Нету Ролей");
        }
        var user = new AppUserEntity(
                dto.getUsername(),
                dto.getPassword(),
                dto.getEmail(),
                role.get()
        );
        var savedUser = userRepository.save(user);
        var userResponseDto = new UserResponseDto(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail()
        );
        return userResponseDto;
    }


}
