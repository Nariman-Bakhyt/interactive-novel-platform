package project.interactivenovelplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.CommentEntity;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    Page<CommentEntity> findByBlock_Id(Long blockId, Pageable pageable);
    Page<CommentEntity> findByChapter_Id(Long chapterId, Pageable pageable);
    Page<CommentEntity> findByNovel_Id(Long novelId, Pageable pageable);

}
