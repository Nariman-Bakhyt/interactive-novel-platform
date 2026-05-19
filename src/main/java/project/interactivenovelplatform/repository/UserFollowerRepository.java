package project.interactivenovelplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.UserFollowerEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFollowerRepository extends JpaRepository<UserFollowerEntity, Long> {
    @EntityGraph(attributePaths = {"sender","receiver"})
    Optional<UserFollowerEntity> findById(Long id);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    @Query("SELECT r FROM UserFollowerEntity r " +
            "WHERE (r.sender.id = :userA AND r.receiver.id = :userB) " +
            "OR (r.sender.id = :userB AND r.receiver.id = :userA)")
    List<UserFollowerEntity> findAllRelationsBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    Boolean existsBySenderIdAndReceiverId(Long userA, Long userB);

    Optional<UserFollowerEntity> findBySenderIdAndReceiverId(Long userA, Long userB);

    @EntityGraph(attributePaths = {"sender"}) 
    Page<UserFollowerEntity> findByReceiverId(Long receiverId, Pageable pageable);

    @EntityGraph(attributePaths = {"receiver"}) 
    Page<UserFollowerEntity> findBySenderId(Long senderId, Pageable pageable);

    @Query("SELECT f.receiver.id , f.id  FROM UserFollowerEntity f WHERE f.sender.id = :senderId")
    List<Object[]> findAllFollowingIds(@Param("senderId") Long senderId);

    @Query("SELECT f.sender.id , f.id FROM UserFollowerEntity f WHERE f.receiver.id = :receiverId")
    List<Object[]> findAllFollowerIds(@Param("receiverId") Long receiverId);
}
