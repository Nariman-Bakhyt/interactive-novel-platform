package project.interactivenovelplatform.security;

import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import project.interactivenovelplatform.entity.AppUserEntity;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {
    private Long id;
    private String username;
    private String password;
    private boolean isActive;
    private boolean isLocked;
    private Collection<? extends GrantedAuthority> authorities;


    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }

    public static UserPrincipal create(AppUserEntity user) {
        List<GrantedAuthority> authorities = user.getRole().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.isActive(),
                user.isLocked(),
                authorities
        );
    }
    public static UserPrincipal createFromClaims(Claims claims) {

        Long id = claims.get("userId", Long.class);
        String username = claims.get("username", String.class);
        boolean isActive = claims.get("isActive", Boolean.class);
        boolean isLocked = claims.get("isLocked", Boolean.class);
        List<?> roles = claims.get("roles", List.class);
        List<SimpleGrantedAuthority> authorities = roles != null
                ? roles.stream()
                  .map(role -> new SimpleGrantedAuthority(role.toString()))
                  .collect(Collectors.toList())
                : List.of();

        return new UserPrincipal(
                id,
                username,
                null, 
                isActive,
                isLocked,
                authorities
        );
    }

}
