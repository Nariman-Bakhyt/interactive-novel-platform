package project.interactivenovelplatform.security;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.entity.Role;

@Component("rsec")
public class RankSecurity {
    public boolean hasRank(Role requiredRole) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal user)) {
            return false;
        }

        int userHighestRank = user.getAuthorities().stream()
                .map(grantedAuthority -> {
                    String roleName = grantedAuthority.getAuthority().replace("ROLE_", "");
                    try {
                        return Role.valueOf(roleName).getRank();
                    } catch (IllegalArgumentException e) {
                        return Integer.MAX_VALUE;
                    }
                })
                .min(Integer::compare)
                .orElse(Integer.MAX_VALUE);

        return userHighestRank <= requiredRole.getRank();
    }
}
