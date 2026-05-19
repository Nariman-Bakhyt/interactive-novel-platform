package project.interactivenovelplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.entity.Novel;
import project.interactivenovelplatform.entity.NovelEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NovelRepository extends JpaRepository<NovelEntity,Long>, JpaSpecificationExecutor<NovelEntity> {
    @Modifying
    @Transactional
    @Query("UPDATE NovelEntity n SET n.viewCount = n.viewCount + :delta WHERE n.id = :novelId")
    void incrementViewCount(@Param("novelId") Long novelId, @Param("delta") Long delta);

    Page<NovelEntity> findByStatusNotInAndIsDeletedFalse(Collection<Novel> status, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<NovelEntity> findAllByStatusNotInAndIsDeletedFalseOrderByPublicationDateDesc(Collection<Novel> status, Pageable pageable);

    Page<NovelEntity> findAllByAuthor_IdAndIsDeletedFalse(Long id, Pageable pageable);

    Optional<NovelEntity> findByAuthor_IdAndIdAndIsDeletedFalse(Long authorId, Long id);

    Page<NovelEntity> findAll(Specification<NovelEntity> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"tags", "genres", "author"})
    List<NovelEntity> findAllByIdIn(Collection<Long> ids);

    boolean existsByIdAndAuthorIdAndIsDeletedFalse(Long id, Long authorId);
    
    interface NovelIdOnly {
        Long getId();
    }
}