package project.interactivenovelplatform.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.dto.response.AuthResponseDto;
import project.interactivenovelplatform.dto.response.JwtAuthenticationResponseDto;
import project.interactivenovelplatform.security.UserPrincipal;

public interface AuthService {
    AuthResponseDto createSession(UserPrincipal user, HttpServletRequest request);

    @Transactional
    AuthResponseDto refreshAccessToken(String refreshToken, String userAgent);

    @Transactional
    void logout(String refreshToken);

    @Transactional
    AuthResponseDto loginByVerificationCode(String email, String code, HttpServletRequest request);
}
