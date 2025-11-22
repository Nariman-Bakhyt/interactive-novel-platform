package project.interactivenovelplatform.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.interactivenovelplatform.dto.request.LoginRequestDto;
import project.interactivenovelplatform.dto.request.RegistrationRequestDto;
import project.interactivenovelplatform.dto.response.UserResponseDto;
import project.interactivenovelplatform.service.UserService;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    // private final JwtTokenProvider tokenProvider; // Будет нужен позже

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody @Valid LoginRequestDto loginRequest){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        // 2. Устанавливаем аутентификацию в контекст (необходимо для работы Security)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Генерируем JWT и помещаем его в куку (КОД ДЛЯ JWT БУДЕТ ДОБАВЛЕН ПОЗЖЕ)
        // String jwt = tokenProvider.generateToken(authentication);
        // ResponseCookie cookie = ResponseCookie.from("JWT_TOKEN", jwt) ...;
        // return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(...);

        // Пока мы не внедрили JWT, просто возвращаем успешный ответ:
        return ResponseEntity.ok("Вход успешен!");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {

        ResponseCookie cookie = ResponseCookie.from("JWT_TOKEN", "") // Имя куки
                .maxAge(0) // Устанавливаем срок действия 0
                .httpOnly(true)
                .path("/")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("Выход выполнен успешно.");
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> registerUser(@RequestBody @Valid RegistrationRequestDto registrationRequestDto){
        var userResponse = userService.registerUser(registrationRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }
}

