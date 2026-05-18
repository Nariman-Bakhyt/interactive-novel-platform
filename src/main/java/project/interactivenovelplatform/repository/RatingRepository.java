package project.interactivenovelplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.RatingEntity;

import java.util.Optional;
@Repository
public interface RatingRepository extends JpaRepository<RatingEntity, Long> {
    Optional<RatingEntity> findByUserIdAndNovelId(Long userId, Long novelId);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT r FROM RatingEntity r WHERE r.novel.id = :novelId")
    Page<RatingEntity> findByNovelId(Long novelId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE NovelEntity n
        SET n.totalScore = n.totalScore + :scoreDiff,
            n.ratingCount = n.ratingCount + :countDiff,
            n.averageRating = CASE WHEN (n.ratingCount + :countDiff) = 0 THEN 0.0 ELSE ROUND(CAST((n.totalScore + :scoreDiff) AS double) / (n.ratingCount + :countDiff) * 100.0) / 100.0 END
        WHERE n.id = :novelId
    """)
    void updateNovelStats(@Param("novelId") Long novelId,
                          @Param("scoreDiff") long scoreDiff,
                          @Param("countDiff") int countDiff);

    @Modifying
    @Query(value = """
    UPDATE novel n SET
        total_score = stat.total,
        rating_count = stat.cnt,
        average_rating = CASE 
            WHEN stat.cnt = 0 THEN 0.0 
            ELSE ROUND((stat.total::numeric / stat.cnt), 2) 
        END
    FROM (
        SELECT 
            COALESCE(SUM(score), 0) AS total,
            COUNT(id) AS cnt
        FROM rating
        WHERE novel_id = :novelId
    ) stat
    WHERE n.id = :novelId
""", nativeQuery = true)
    void recalculateTotalStats(@Param("novelId") Long novelId);
}