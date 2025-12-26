package project.interactivenovelplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.interactivenovelplatform.entity.Novel;
import project.interactivenovelplatform.entity.NovelEntity;

import java.util.Collection;
import java.util.Optional;

public interface NovelRepository extends JpaRepository<NovelEntity,Long>, JpaSpecificationExecutor<NovelEntity> {
    @Modifying
    @Query("UPDATE NovelEntity n SET n.viewCount = n.viewCount + 1 WHERE n.id = :novelId")
    int incrementViewCount(@Param("novelId") Long novelId);

    Page<NovelEntity> findByStatusNotIn(Collection<Novel> status, Pageable pageable);
    Page<NovelEntity>  findAllByStatusNotInOrderByPublicationDateDesc (Collection<Novel> status, Pageable pageable);
    Page<NovelEntity> findAllByAuthor_Id(Long id, Pageable pageable);
    Optional<NovelEntity> findByAuthor_IdAndId(Long authorId, Long id);
}
