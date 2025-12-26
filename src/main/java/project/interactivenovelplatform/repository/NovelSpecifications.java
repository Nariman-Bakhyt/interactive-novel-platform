package project.interactivenovelplatform.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import project.interactivenovelplatform.entity.GenreEntity;
import project.interactivenovelplatform.entity.Novel;
import project.interactivenovelplatform.entity.NovelEntity;
import project.interactivenovelplatform.entity.TagEntity;


import java.math.BigDecimal;
import java.util.Collection;

public class NovelSpecifications {
    public static Specification<NovelEntity> hasRatingInRange(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("averageRating"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("averageRating"), min);
            return cb.lessThanOrEqualTo(root.get("averageRating"), max);
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

    public static Specification<NovelEntity> hasAllGenres(Collection<String> genresNames) {
        return (root, query, cb) ->{
            if (genresNames == null || genresNames.isEmpty() ) return null;
            Predicate[] predicates = genresNames.stream().map(
                    name -> {
                        Join<NovelEntity, GenreEntity> genreJoin = root.join("genres");
                        return cb.equal(genreJoin.get("name"), name);
                    })
                    .toArray(Predicate[]::new);
            return cb.and(predicates);
        };
    }

    public  static Specification<NovelEntity> hasAllTags(Collection<String> tagNames){
        return (root, query, cb) -> {
            if (tagNames == null || tagNames.isEmpty() ) return null;
            Predicate[] predicates = tagNames.stream().map(
                    name->{
                        Join<NovelEntity, TagEntity> tagJoin = root.join("tags");
                        return cb.equal(tagJoin.get("name"), name);
                    })
                    .toArray(Predicate[]::new);
            return cb.and(predicates);

        };

    }

    public static Specification<NovelEntity> hasAllGenreIds(Collection<Long> genreIds) {
        return (root, query, cb) -> {
            if (genreIds == null || genreIds.isEmpty()) return null;

            Predicate[] predicates = genreIds.stream()
                    .map(id -> {
                        Join<NovelEntity, GenreEntity> genreJoin = root.join("genres");
                        return cb.equal(genreJoin.get("id"), id);
                    })
                    .toArray(Predicate[]::new);

            return cb.and(predicates);
        };
    }

    public static Specification<NovelEntity> hasAllTagIds(Collection<Long> tagIds) {
        return (root, query, cb) -> {
            if (tagIds == null || tagIds.isEmpty()) return null;

            // Для каждого ID создаем отдельный Join, чтобы реализовать логику "И"
            Predicate[] predicates = tagIds.stream()
                    .map(id -> {
                        Join<NovelEntity, TagEntity> genreJoin = root.join("tags");
                        return cb.equal(genreJoin.get("id"), id);
                    })
                    .toArray(Predicate[]::new);

            return cb.and(predicates);
        };
    }
}
