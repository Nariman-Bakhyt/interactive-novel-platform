package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.domain.Specification;
import project.interactivenovelplatform.entity.AppUserEntity;

public class UserSpecifications {
    public static Specification<AppUserEntity> UserNameLike(String username){
        return (root, query, cb) ->{
            if(username==null || username.isBlank()) return null;
            return cb.like(cb.upper(root.get("username")), "%" + username.toUpperCase() + "%");
        }  ;
    }
}
