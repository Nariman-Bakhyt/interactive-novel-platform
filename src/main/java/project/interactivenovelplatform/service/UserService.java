package project.interactivenovelplatform.service;

import project.interactivenovelplatform.dto.request.RegistrationRequestDto;
import project.interactivenovelplatform.dto.response.UserResponseDto;

import java.util.List;


public interface UserService {
    List<UserResponseDto> findAll();
    UserResponseDto findByUsername(String username);
    UserResponseDto registerUser( RegistrationRequestDto dto);
}
