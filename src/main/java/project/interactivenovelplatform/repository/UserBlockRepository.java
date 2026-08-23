package project.interactivenovelplatform.repository;

import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.UserBlockEntity;

import java.util.List;
import java.util.Set;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlockEntity, Long> {
    boolean existsUserBlockEntityByBlockerIdAndBlockedId (Long blockerId, Long blockedId) ;
    @Query("""
        SELECT COUNT(b) > 0 FROM UserBlockEntity b
        WHERE (b.blocker.id = :u1 AND b.blocked.id = :u2)
           OR (b.blocker.id = :u2 AND b.blocked.id = :u1)
    """)
    boolean isBlockedEitherWay(@Param("u1") Long u1, @Param("u2") Long u2);
    @EntityGraph(attributePaths = {"blocked"})
    Slice<UserBlockEntity> findAllByBlockerId(Long blockerId, Pageable pageable);
    @Query("""
    SELECT b.blocked.id  FROM UserBlockEntity b WHERE b.blocker.id = :userId
""")
    List<Object[]> findAllBlockedIds(@Param("userId") Long userId);

    @Query("""
        SELECT ub.blocker.id as blockerId, ub.blocked.id as blockedId
        FROM UserBlockEntity ub
        WHERE (ub.blocked.id = :myId AND ub.blocker.id IN :opponentIds)
           OR (ub.blocker.id = :myId AND ub.blocked.id IN :opponentIds)
    """)
    List<BlockInfo> findAllBlockInfoBetween(@Param("myId") Long myId, @Param("opponentIds") Set<Long> opponentIds);

    
    interface BlockInfo {
        Long getBlockerId();
        Long getBlockedId();
    }
}
