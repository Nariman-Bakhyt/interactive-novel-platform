package project.interactivenovelplatform.security;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.entity.Role;

@Component("rsec")
public class RankSecurity {
    public int getHighestRoleRank(AppUserEntity user) {
        if (user == null || user.getRole() == null) return Integer.MAX_VALUE;

        return user.getRole().stream()
                .map(roleEntity -> roleEntity.getName().getRank())
                .min(Integer::compare)
                .orElse(Integer.MAX_VALUE);
    }

    public boolean hasRank(Role requiredRole) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof AppUserEntity user)) {
            return false;
        }

        return getHighestRoleRank(user) <= requiredRole.getRank();
    }
}
