package project.interactivenovelplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.interactivenovelplatform.entity.RelationStatus;
import project.interactivenovelplatform.entity.UserFriendEntity;

import java.util.List;
import java.util.Optional;

public interface UserFriendRepository extends JpaRepository<UserFriendEntity, Long> {

    @Query("""
    SELECT (COUNT(f) > 0) FROM UserFriendEntity f
    WHERE (f.sender.id = :userA AND f.receiver.id = :userB)
       OR (f.sender.id = :userB AND f.receiver.id = :userA)
    """)
    boolean existsFriendshipBetween(@Param("userA") Long userA, @Param("userB") Long userB);
    @Query("""
    SELECT (COUNT(f) > 0) FROM UserFriendEntity f
    WHERE ((f.sender.id = :userA AND f.receiver.id = :userB)
       OR (f.sender.id = :userB AND f.receiver.id = :userA))
    AND f.status = project.interactivenovelplatform.entity.RelationStatus.FRIEND
    """)
    boolean existsIsFriend(@Param("userA") Long userA, @Param("userB") Long userB);

    @Query("""
        SELECT f FROM UserFriendEntity f
        WHERE (f.sender.id = :id1 AND f.receiver.id = :id2)
           OR (f.sender.id = :id2 AND f.receiver.id = :id1)
        """)
    Optional<UserFriendEntity> findRelation(@Param("id1") Long id1, @Param("id2") Long id2);

    Optional<UserFriendEntity> findUserFriendBySenderIdAndReceiverIdAndStatus(Long senderId, Long receiverId, RelationStatus status);
    Optional<UserFriendEntity> findUserFriendByIdAndStatusAndReceiverId(Long id, RelationStatus status, Long receiverId);

    @EntityGraph(attributePaths = {"sender"}) // Качаем тех, кто кинул заявку МНЕ
    Page<UserFriendEntity> findByReceiverIdAndStatus(Long receiverId, RelationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"receiver"})
    Page<UserFriendEntity> findBySenderIdAndStatus(Long senderId, RelationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "receiver"}) // Качаем друзей (обе стороны)
    @Query("SELECT f FROM UserFriendEntity f WHERE (f.sender.id = :userId OR f.receiver.id = :userId) AND f.status = :status")
    Page<UserFriendEntity> findAllFriendsByUserId(@Param("userId") Long userId, @Param("status") RelationStatus status, Pageable pageable);

    @Query("""
        SELECT CASE WHEN f.sender.id = :userId THEN f.receiver.id ELSE f.sender.id  END , f.id
        FROM UserFriendEntity f
        WHERE (f.sender.id = :userId OR f.receiver.id = :userId)
        AND f.status =  project.interactivenovelplatform.entity.RelationStatus.FRIEND
""")
    List<Object[]> findAllFriendIdsByUserId(@Param("userId") Long userId);


    @Query("""
        SELECT f.sender.id , f.id FROM UserFriendEntity f WHERE f.receiver.id = :userId
        AND f.status = project.interactivenovelplatform.entity.RelationStatus.PENDING
""")
    List<Object[]> findAllIncomingRequests(@Param("userId") Long userId);

    @Query("""
        SELECT f.receiver.id , f.id FROM UserFriendEntity f WHERE f.sender.id = :userId
        AND f.status = project.interactivenovelplatform.entity.RelationStatus.PENDING
""")
    List<Object[]> findAllOutgoingRequests(@Param("userId") Long userId);
}
