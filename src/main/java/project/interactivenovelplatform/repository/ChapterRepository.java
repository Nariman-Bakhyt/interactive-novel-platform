package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.dto.response.ChapterShortResponseDto;
import project.interactivenovelplatform.entity.ChapterEntity;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<ChapterEntity, Long> {
    @Query("SELECT new project.interactivenovelplatform.dto.response.ChapterShortResponseDto(c.id,c.chapterNumber,c.title) " +
            "FROM ChapterEntity c WHERE c.novel.id = :novelId ORDER BY c.chapterNumber ASC")
    List<ChapterShortResponseDto> findAllByNovelIdShort(@Param("novelId") Long novelId);

    Optional<ChapterEntity> findByNovelIdAndId(Long novelId, Long id);
    Optional<ChapterEntity> findByNovel_IdAndTitleIgnoreCase(Long novelId, String title);
    @Query("SELECT MAX(c.chapterNumber) FROM ChapterEntity c WHERE c.novel.id = :novelId")
    Optional<Double> findMaxChapterNumberByNovelId(@Param("novelId") Long novelId);

    @Modifying
    @Transactional
    @Query("UPDATE ChapterEntity c SET c.chapterNumber = :newPos " +
            "WHERE c.id = :chapterId AND c.novel.id = :novelId")
    void updatePositionSecurely(
            @Param("chapterId") Long chapterId,
            @Param("novelId") Long novelId,
            @Param("newPos") Double newPos
    );
}
