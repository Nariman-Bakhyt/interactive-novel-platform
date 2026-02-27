package project.interactivenovelplatform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import project.interactivenovelplatform.entity.AppUserEntity;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;


@Component
public class JwtTokenProvider {
    private final String jwtSecret;
    private final long jwtExpiration;
    private final SecretKey key;

    // 1. Внедряем значения из application.properties через конструктор
    public JwtTokenProvider(@Value("${jwt.secret}") String jwtSecret,
                            @Value("${jwt.expiration}") long jwtExpiration) {
        this.jwtSecret = jwtSecret;
        this.jwtExpiration = jwtExpiration;

        // 2. Инициализируем Key, используя внедренное значение
        // Используем Decoders.BASE64, предполагая, что ключ в properties закодирован в Base64
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));

    }
    // 1. Генерация Токена (вызывается при входе)
    public String generateToken(Authentication authentication) {
        AppUserEntity userPrincipal = (AppUserEntity) authentication.getPrincipal();

        Long userId = userPrincipal.getId();
        String username = userPrincipal.getUsername();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(userId.toString())
                // Кастомные утверждения для удобства клиента и авторизации:
                .claim("userId",userId)
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(this.key)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(this.key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Возвращаем userId
        return claims.get("userId", Long.class);
    }

    // 3. Получение username пользователя из токена
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(this.key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Возвращаем username
        return claims.get("username", String.class);
    }

    // 4. Проверка валидности
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(this.key)
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Логирование ошибки
        }
        return false;
    }

}
