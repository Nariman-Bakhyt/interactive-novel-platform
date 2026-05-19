package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.domain.Specification;
import project.interactivenovelplatform.entity.AppUserEntity;

public class UserSpecifications {
    public static Specification<AppUserEntity> userNameLike(String username) {
        return (root, query, cb) -> {
            if (username == null || username.isBlank()) return null;

            String pattern = "%" + username.toUpperCase() + "%";
            String startPattern = username.toUpperCase() + "%";

            
            var likePredicate = cb.like(cb.upper(root.get("username")), pattern);

            
            
            var caseExpression = cb.selectCase()
                    .when(cb.like(cb.upper(root.get("username")), startPattern), 0)
                    .otherwise(1);

            
            query.orderBy(
                    cb.asc(caseExpression),           
                    cb.asc(root.get("username"))     
            );

            return likePredicate;
        };
    }
}
