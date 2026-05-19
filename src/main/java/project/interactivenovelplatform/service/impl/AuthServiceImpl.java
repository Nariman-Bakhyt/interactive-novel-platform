package project.interactivenovelplatform.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.dto.response.AuthResponseDto;
import project.interactivenovelplatform.dto.response.JwtAuthenticationResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.entity.UserSessionEntity;
import project.interactivenovelplatform.entity.VerificationTokenType;
import project.interactivenovelplatform.repository.UserRepository;
import project.interactivenovelplatform.repository.UserSessionRepository;
import project.interactivenovelplatform.security.JwtTokenProvider;
import project.interactivenovelplatform.security.UserPrincipal;
import project.interactivenovelplatform.service.AuthService;
import project.interactivenovelplatform.service.VerificationService;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserSessionRepository userSessionRepository;
    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;

    private final UserRepository userRepository;
    private final VerificationService verificationService;

    @Transactional
    @Override
    public AuthResponseDto createSession(UserPrincipal user, HttpServletRequest request) {
        String accessToken = tokenProvider.generateAccessToken(user);
        String signedRefreshToken = tokenProvider.generateSignedRefreshToken();

        String userAgent = request.getHeader("User-Agent");
        String ipAddress = request.getRemoteAddr();

        UserSessionEntity session = userSessionRepository.findByUserIdAndUserAgent(user.getId(), userAgent)
                .orElseGet(UserSessionEntity::new);

        if (session.getRefreshToken() != null) {
            redisTemplate.delete("refresh:" + session.getRefreshToken());
        }

        session.setUserId(user.getId());
        session.setUserAgent(userAgent);
        session.setRefreshToken(signedRefreshToken);
        session.setIpAddress(ipAddress);
        session.setLoginTime(OffsetDateTime.now());
        session.setExpiresAt(OffsetDateTime.now().plusDays(30)); 
        session.setActive(true);
        userSessionRepository.save(session);

        String redisKey = "refresh:" + signedRefreshToken;
        Map<String, String> sessionData = Map.of(
                "userId", user.getId().toString(),
                "userAgent", userAgent
        );
        redisTemplate.opsForHash().putAll(redisKey, sessionData);
        redisTemplate.expire(redisKey, 30, TimeUnit.DAYS);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", signedRefreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/api/auth")
                .maxAge(30 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();
        ResponseCookie deleteGuestCookie = ResponseCookie.from("guest_id", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0) 
                .build();

        return new AuthResponseDto(accessToken, user.getUsername(), cookie ,deleteGuestCookie, user.getUsername());
    }

    @Transactional
    @Override
    public JwtAuthenticationResponseDto refreshAccessToken(String refreshToken, String userAgent) {
        if (refreshToken == null || !tokenProvider.verifyRefreshTokenSignature(refreshToken)) {
            throw new BadCredentialsException("Невалидный токен обновления");
        }

        String redisKey = "refresh:" + refreshToken;
        String userIdStr = (String) redisTemplate.opsForHash().get(redisKey, "userId");

        if (userIdStr == null) {
            UserSessionEntity session = userSessionRepository.findByRefreshToken(refreshToken)
                    .filter(UserSessionEntity::isActive)
                    .filter(s -> s.getExpiresAt().isAfter(OffsetDateTime.now()))
                    .orElseThrow(() -> new BadCredentialsException("Сессия истекла или не существует"));

            userIdStr = session.getUserId().toString();

            redisTemplate.opsForHash().put(redisKey, "userId", userIdStr);
            redisTemplate.opsForHash().put(redisKey, "userAgent", userAgent);
            redisTemplate.expire(redisKey, 30, TimeUnit.DAYS);
        }

        Long userId = Long.parseLong(userIdStr);
        AppUserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        UserPrincipal userPrincipal = UserPrincipal.create(userEntity);

        String newAccessToken = tokenProvider.generateAccessToken(userPrincipal);

        return new JwtAuthenticationResponseDto(newAccessToken, userPrincipal.getUsername());
    }

    @Transactional
    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null) return;

        redisTemplate.delete("refresh:" + refreshToken);

        userSessionRepository.findByRefreshToken(refreshToken).ifPresent(session -> {
            session.setActive(false);
            userSessionRepository.save(session);
        });
    }

    @Transactional
    @Override
    public AuthResponseDto loginByVerificationCode(String email, String code, HttpServletRequest request) {
        AppUserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь с такой почтой не найден"));

        verificationService.verifyCode(user.getId(), code, VerificationTokenType.LOGIN_BY_CODE);

        UserPrincipal principal = UserPrincipal.create(user);
        return createSession(principal, request);
    }

}
