package project.interactivenovelplatform.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String path = request.getRequestURI();

            if (path.contains("/auth/") || path.startsWith("/ws")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = getJwtFromRequest(request);

            if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
                Claims claims = tokenProvider.getClaimsFromToken(token);
                // Создаем UserPrincipal напрямую из JWT claims. Это исключает обращение к базе данных при каждом HTTP-запросе, обеспечивая полноценный stateless-режим.
                UserPrincipal userDetails = UserPrincipal.createFromClaims(claims);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
            // Не выбрасываем исключение сразу, чтобы публичные эндпоинты (public/**) могли корректно работать, просто деградируя сессию до анонимной. Фильтрацию приватных маршрутов берет на себя securityFilterChain.
            request.setAttribute("JWT_EXPIRED_OR_INVALID", true);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}