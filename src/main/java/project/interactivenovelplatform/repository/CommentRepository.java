package project.interactivenovelplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.CommentEntity;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    Page<CommentEntity> findByBlock_IdAndIsDeletedIsFalse(Long blockId, Pageable pageable);
    Page<CommentEntity> findByChapter_IdAndIsDeletedFalse(Long chapterId, Pageable pageable);
    Page<CommentEntity> findByNovel_IdAndIsDeletedFalse(Long novelId, Pageable pageable);

}
