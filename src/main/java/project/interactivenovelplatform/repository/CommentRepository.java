package project.interactivenovelplatform.repository;

import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.CommentEntity;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    @EntityGraph(attributePaths = {"user"})
    Slice<CommentEntity> findByBlock_Id(Long blockId, Pageable pageable);
    @EntityGraph(attributePaths = {"user"})
    Slice<CommentEntity> findByChapter_Id(Long chapterId, Pageable pageable);
    @EntityGraph(attributePaths = {"user"})
    Slice<CommentEntity> findByNovel_Id(Long novelId, Pageable pageable);
    @EntityGraph(attributePaths = {"user"})
    Slice<CommentEntity> findByForumTopic_Id(Long forumTopicId, Pageable pageable);
    @EntityGraph(attributePaths = {"user"})
    Slice<CommentEntity> findByChannelPost_Id(Long channelPostId, Pageable pageable);
    @EntityGraph(attributePaths = {"user"})
    Optional<CommentEntity> findById(Long commentId);
}
