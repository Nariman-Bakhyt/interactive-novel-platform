package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.interactivenovelplatform.entity.UserCloseFriendsEntity;

import java.util.List;
import java.util.Optional;

public interface UserCloseFriendsRepository extends JpaRepository<UserCloseFriendsEntity, Long> {
    boolean existsByOwnerIdAndFriendId(Long ownerId, Long friendId);
    @EntityGraph(attributePaths = {"friend"})
    List<UserCloseFriendsEntity> findAllByOwnerId(Long ownerId);
    @EntityGraph(attributePaths = {"friend"})
    Optional<UserCloseFriendsEntity> findByOwnerIdAndFriendId(Long ownerId, Long friendId);

    @Query("""
        SELECT f.friend.id , f.id FROM UserCloseFriendsEntity f WHERE f.owner.id = :userId
""")
    List<Object[]> findCloseFriendIds(@Param("userId")  Long userId);
}
