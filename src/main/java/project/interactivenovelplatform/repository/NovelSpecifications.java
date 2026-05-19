package project.interactivenovelplatform.repository;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import project.interactivenovelplatform.entity.GenreEntity;
import project.interactivenovelplatform.entity.Novel;
import project.interactivenovelplatform.entity.NovelEntity;
import project.interactivenovelplatform.entity.TagEntity;

import java.util.Collection;

public class NovelSpecifications {
    public static Specification<NovelEntity> hasRatingInRange(Double min, Double max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            Path<Double> avgRating = root.get("averageRating");

            if (min != null && max != null) return cb.between(avgRating, min, max);
            if (min != null) return cb.greaterThanOrEqualTo(avgRating, min);
            return cb.lessThanOrEqualTo(avgRating, max);
        };
    }

    public static Specification<NovelEntity> hasAuthor(Long authorId) {
        return (root, query, cb) -> {
            if (authorId == null) return null;
            return cb.equal(root.get("author").get("id"), authorId);
        };
    }

    public static Specification<NovelEntity> hasStatus(Novel status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }


    public static Specification<NovelEntity> filterByTags(Collection<Long> includedIds, Collection<Long> excludedIds) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if(includedIds != null && !includedIds.isEmpty()) {
                // Используем subquery с группировкой и HAVING, чтобы реализовать логику строгого "И" (AND) для тегов. Обычный JOIN применил бы "ИЛИ" (OR).
                Subquery<Long> subqueryInclude = query.subquery(Long.class);
                Root<NovelEntity> subRootInclude = subqueryInclude.from(NovelEntity.class);
                Join<NovelEntity, TagEntity> tagJoin = subRootInclude.join("tags");

                subqueryInclude.select(subRootInclude.get("id"))
                        .where(tagJoin.get("id").in(includedIds))
                        .groupBy(subRootInclude.get("id"))
                        .having(cb.equal(cb.count(tagJoin), (long) includedIds.size()));

                predicate = cb.and(predicate, root.get("id").in(subqueryInclude));
            }

            if (excludedIds != null && !excludedIds.isEmpty()) {
                // Исключаем новеллы с запрещенными тегами через NOT IN подзапрос. Прямое исключение в JOIN вернет ложные совпадения из-за наличия других тегов.
                Subquery<Long> subqueryExclude = query.subquery(Long.class);
                Root<NovelEntity> subRootExclude = subqueryExclude.from(NovelEntity.class);
                Join<NovelEntity, TagEntity> tagJoin = subRootExclude.join("tags");

                subqueryExclude.select(subRootExclude.get("id"))
                        .where(tagJoin.get("id").in(excludedIds));

                predicate = cb.and(predicate, cb.not(root.get("id").in(subqueryExclude)));
            }

            return predicate;

        };
    }

    public static Specification<NovelEntity> filterByGenres(Collection<Long> includedIds, Collection<Long> excludedIds) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (includedIds != null && !includedIds.isEmpty()) {
                Subquery<Long> subqueryInclude = query.subquery(Long.class);
                Root<NovelEntity> subRootInclude = subqueryInclude.from(NovelEntity.class);
                Join<NovelEntity, GenreEntity> genreJoin = subRootInclude.join("genres");

                subqueryInclude.select(subRootInclude.get("id"))
                        .where(genreJoin.get("id").in(includedIds))
                        .groupBy(subRootInclude.get("id"))
                        .having(cb.equal(cb.count(genreJoin), (long) includedIds.size()));

                predicate = cb.and(predicate, root.get("id").in(subqueryInclude));
            }

            if (excludedIds != null && !excludedIds.isEmpty()) {
                Subquery<Long> subqueryExclude = query.subquery(Long.class);
                Root<NovelEntity> subRootExclude = subqueryExclude.from(NovelEntity.class);
                Join<NovelEntity, GenreEntity> genreJoin = subRootExclude.join("genres");

                subqueryExclude.select(subRootExclude.get("id"))
                        .where(genreJoin.get("id").in(excludedIds));

                predicate = cb.and(predicate, cb.not(root.get("id").in(subqueryExclude)));
            }

            return predicate;
        };
    }

    public static Specification<NovelEntity> titleLike(String title){
        return ((root, query, cb) ->{
            if (title == null || title.isBlank()) return null;

            String pattern = "%" + title.toUpperCase() + "%";
            String startPattern = title.toUpperCase() + "%";
            var likePredicate = cb.like(cb.upper(root.get("title")), pattern);

            // Вычисляем приоритет сортировки: заголовки, начинающиеся со строки поиска, выводятся первыми (caseExpression = 0),
            // а совпадения в середине строки — вторыми (caseExpression = 1). Это выносит логику релевантности на уровень СУБД.
            var caseExpression = cb.selectCase()
                    .when(cb.like(cb.upper(root.get("title")), startPattern), 0)
                    .otherwise(1);

            query.orderBy(
                    cb.asc(caseExpression),           
                    cb.asc(root.get("title"))     
            );
            return likePredicate;
        } );
    }

    public static Specification<NovelEntity> isNotDeleted() {
        return (root, query, cb) -> cb.equal(root.get("isDeleted"), false);
    }

}