package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.domain.Specification;
import project.interactivenovelplatform.entity.AppUserEntity;

public class UserSpecifications {
    public static Specification<AppUserEntity> userNameLike(String username) {
        return (root, query, cb) -> {
            if (username == null || username.isBlank()) return null;

            String pattern = "%" + username.toUpperCase() + "%";
            String startPattern = username.toUpperCase() + "%";

            // 1. Предикат для фильтрации (оставляем как было)
            var likePredicate = cb.like(cb.upper(root.get("username")), pattern);

            // 2. Логика сортировки: сначала те, что начинаются на search
            // Если начинается с search -> 0, иначе -> 1
            var caseExpression = cb.selectCase()
                    .when(cb.like(cb.upper(root.get("username")), startPattern), 0)
                    .otherwise(1);

            // Добавляем Order в query
            query.orderBy(
                    cb.asc(caseExpression),           // Сначала совпадения в начале
                    cb.asc(root.get("username"))     // Затем по алфавиту для одинаковых групп
            );

            return likePredicate;
        };
    }
}
