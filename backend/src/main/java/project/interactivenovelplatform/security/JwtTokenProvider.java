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

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;


@Component
public class JwtTokenProvider {
    private final String jwtSecret;
    private final long jwtExpiration;
    private final SecretKey key;

    
    public JwtTokenProvider(@Value("${jwt.secret}") String jwtSecret,
                            @Value("${jwt.expiration}") long jwtExpiration) {
        this.jwtSecret = jwtSecret;
        this.jwtExpiration = jwtExpiration;

        
        
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));

    }
    
    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateAccessToken(userPrincipal); 
    }
    public String generateAccessToken(UserPrincipal userPrincipal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 900000); 
        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return Jwts.builder()
                .subject(userPrincipal.getId().toString())
                .claim("userId", userPrincipal.getId())
                .claim("username", userPrincipal.getUsername())
                .claim("isActive", userPrincipal.isActive())
                .claim("isLocked", userPrincipal.isLocked())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(this.key)
                .compact();
    }

    public String generateSignedRefreshToken() {
        String uuid = UUID.randomUUID().toString();
        String signature = calculateHmac(uuid);
        return uuid + "." + signature;
    }

    public boolean verifyRefreshTokenSignature(String tokenWithSignature) {
        // Быстрая верификация подписи токена обновления по HMAC. Позволяет отсекать некорректные токены на раннем этапе (fast-fail), защищая БД/хранилище от спама невалидными запросами.
        String[] parts = tokenWithSignature.split("\\.");
        if (parts.length != 2) return false;

        String uuid = parts[0];
        String signature = parts[1];

        
        return signature.equals(calculateHmac(uuid));
    }

    
    private String calculateHmac(String data) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(
                    jwtSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            sha256_HMAC.init(secret_key);

            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC signature", e);
        }
    }

    public String generateTokenFromUserId(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 900000); 

        return Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId)
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

        
        return claims.get("userId", Long.class);
    }

    
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(this.key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        
        return claims.get("username", String.class);
    }

    
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(this.key)
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            
        }
        return false;
    }
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
