package project.interactivenovelplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.interactivenovelplatform.entity.RatingEntity;

import java.util.Optional;

public interface RatingRepository extends JpaRepository<RatingEntity, Long> {
    Optional<RatingEntity> findByUserIdAndNovelId(Long userId, Long novelId);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT r FROM RatingEntity r WHERE r.novel.id = :novelId")
    Page<RatingEntity> findByNovelId(Long novelId, Pageable pageable);
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE NovelEntity n
        SET n.totalScore = n.totalScore + :scoreDiff,
            n.ratingCount = n.ratingCount + :countDiff
        WHERE n.id = :novelId
    """)
    void updateNovelStats(@Param("novelId") Long novelId,
                          @Param("scoreDiff") long scoreDiff,
                          @Param("countDiff") int countDiff);
    @Modifying
    @Query("""
        UPDATE NovelEntity n SET
        n.totalScore = (SELECT COALESCE(SUM(r.score), 0) FROM RatingEntity r WHERE r.novel.id = n.id),
        n.ratingCount = (SELECT COUNT(r) FROM RatingEntity r WHERE r.novel.id = n.id)
        WHERE n.id = :novelId
    """)
    void recalculateTotalStats(@Param("novelId") Long novelId);
}
