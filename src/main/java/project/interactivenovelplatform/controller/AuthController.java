package project.interactivenovelplatform.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import project.interactivenovelplatform.dto.request.LoginRequestDto;
import project.interactivenovelplatform.dto.request.RegistrationRequestDto;
import project.interactivenovelplatform.dto.response.JwtAuthenticationResponseDto;
import project.interactivenovelplatform.entity.UserSessionEntity;
import project.interactivenovelplatform.repository.UserSessionRepository;
import project.interactivenovelplatform.security.JwtTokenProvider;
import project.interactivenovelplatform.security.UserPrincipal;
import project.interactivenovelplatform.service.UserService;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final UserDetailsService userDetailsService;
    private final UserSessionRepository userSessionRepository;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody @Valid LoginRequestDto loginRequest, HttpServletRequest request){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        // 2. Устанавливаем аутентификацию в контекст (необходимо для работы Security)
        SecurityContextHolder.getContext().setAuthentication(authentication);


        String accessToken = tokenProvider.generateAccessToken(authentication);
        String signedRefreshToken = tokenProvider.generateSignedRefreshToken();

        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        // Сохраняем userId и метаданные (на 30 дней)
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("userId", user.getId().toString());
        sessionData.put("userAgent", request.getHeader("User-Agent"));
        String redisKey = "refresh:" + signedRefreshToken;

        redisTemplate.opsForHash().putAll(redisKey, sessionData);
        redisTemplate.expire(redisKey, 30, java.util.concurrent.TimeUnit.DAYS);

        UserSessionEntity session = UserSessionEntity.builder()
                .userId(user.getId())
                .refreshToken(signedRefreshToken)
                .userAgent(request.getHeader("User-Agent"))
                .ipAddress(request.getRemoteAddr())
                .loginTime(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusDays(30))
                .isActive(true)
                .build();

        userSessionRepository.save(session);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", signedRefreshToken)
                .httpOnly(true)
                .secure(false) // Для локальной разработки false, для продакшена (HTTPS) true
                .path("/api/auth") // Кука будет лететь только на эндпоинт обновления
                .maxAge(30 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();

        // 4. Возвращаем Access Token в теле ответа
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new JwtAuthenticationResponseDto(accessToken, user.getUsername()));
    }


    @PostMapping("/refresh")
    public ResponseEntity<?> refreshTokens(@CookieValue(name = "refreshToken", required = false) String refreshToken,
                                           HttpServletRequest request) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token missing");
        }
        if (!tokenProvider.verifyRefreshTokenSignature(refreshToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid token signature");
        }

        String redisKey = "refresh:" + refreshToken;
        String userId = (String) redisTemplate.opsForHash().get(redisKey, "userId");

        if (userId == null) {
            UserSessionEntity session = userSessionRepository.findByRefreshToken(refreshToken)
                    .filter(UserSessionEntity::isActive)
                    .filter(s -> s.getExpiresAt().isAfter(OffsetDateTime.now()))
                    .orElse(null);

            if (session == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Session expired or invalid");
            }

            userId = session.getUserId().toString();

            Map<String, String> sessionData = new HashMap<>();
            sessionData.put("userId", userId);
            sessionData.put("userAgent", request.getHeader("User-Agent"));
            redisTemplate.opsForHash().putAll(redisKey, sessionData);
            redisTemplate.expire(redisKey, 30, java.util.concurrent.TimeUnit.DAYS);
        }

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );

            String newAccessToken = tokenProvider.generateAccessToken(auth);
            return ResponseEntity.ok(new JwtAuthenticationResponseDto(newAccessToken, userDetails.getUsername()));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken != null) {
            redisTemplate.delete("refresh:" + refreshToken);
            userSessionRepository.findByRefreshToken(refreshToken).ifPresent(session -> {
                session.setActive(false);
                userSessionRepository.save(session);
            });
        }

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .maxAge(0)
                .httpOnly(true)
                .path("/api/auth/refresh")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody @Valid RegistrationRequestDto registrationRequestDto){
        userService.registerUser(registrationRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}

