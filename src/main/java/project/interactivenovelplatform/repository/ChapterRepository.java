package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.dto.response.ChapterShortResponseDto;
import project.interactivenovelplatform.entity.ChapterEntity;
import project.interactivenovelplatform.entity.ChapterStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<ChapterEntity, Long> {
    // Список для автора (все активные главы)
    @Query("SELECT new project.interactivenovelplatform.dto.response.ChapterShortResponseDto(c.id, c.chapterNumber, c.title, c.status, c.publishedAt) " +
            "FROM ChapterEntity c WHERE c.novel.id = :novelId AND c.isDeleted = false ORDER BY c.chapterNumber ASC")
    List<ChapterShortResponseDto> findAllByNovelIdShort(@Param("novelId") Long novelId);

    // Публичный список (только активные опубликованные главы)
    @Query("SELECT new project.interactivenovelplatform.dto.response.ChapterShortResponseDto(c.id, c.chapterNumber, c.title, c.status, c.publishedAt) " +
            "FROM ChapterEntity c WHERE c.novel.id = :novelId AND c.status = project.interactivenovelplatform.entity.ChapterStatus.PUBLISHED AND c.isDeleted = false ORDER BY c.chapterNumber ASC")
    List<ChapterShortResponseDto> findAllPublishedByNovelIdShort(@Param("novelId") Long novelId);

    // Подсчет активных опубликованных глав новеллы
    @Query("SELECT COUNT(c) FROM ChapterEntity c WHERE c.novel.id = :novelId AND c.status = project.interactivenovelplatform.entity.ChapterStatus.PUBLISHED AND c.isDeleted = false")
    long countPublishedChapters(@Param("novelId") Long novelId);

    // Дата публикации последней активной опубликованной главы
    @Query("SELECT MAX(c.publishedAt) FROM ChapterEntity c WHERE c.novel.id = :novelId AND c.status = project.interactivenovelplatform.entity.ChapterStatus.PUBLISHED AND c.isDeleted = false")
    Optional<OffsetDateTime> findLatestPublishedDate(@Param("novelId") Long novelId);

    // Поиск активных глав для автопубликации планировщиком
    List<ChapterEntity> findAllByStatusAndPublishedAtBeforeAndIsDeletedFalse(ChapterStatus status, OffsetDateTime now);

    Optional<ChapterEntity> findByNovelIdAndIdAndIsDeletedFalse(Long novelId, Long id);
    Optional<ChapterEntity> findByNovel_IdAndTitleIgnoreCaseAndIsDeletedFalse(Long novelId, String title);

    @Query("SELECT MAX(c.chapterNumber) FROM ChapterEntity c WHERE c.novel.id = :novelId AND c.isDeleted = false")
    Optional<Double> findMaxChapterNumberByNovelId(@Param("novelId") Long novelId);

    @Modifying
    @Transactional
    @Query("UPDATE ChapterEntity c SET c.chapterNumber = :newPos " +
            "WHERE c.id = :chapterId AND c.novel.id = :novelId AND c.isDeleted = false")
    void updatePositionSecurely(
            @Param("chapterId") Long chapterId,
            @Param("novelId") Long novelId,
            @Param("newPos") Double newPos
    );
}
