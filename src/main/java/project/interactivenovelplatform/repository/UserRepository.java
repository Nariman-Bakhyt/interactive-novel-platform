package project.interactivenovelplatform.repository;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.dto.response.RelationshipStateDto;
import project.interactivenovelplatform.dto.response.ProfileResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.entity.RelationStatus;
import project.interactivenovelplatform.entity.Novel;

import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<AppUserEntity, Long>, JpaSpecificationExecutor<AppUserEntity> {

    Optional<AppUserEntity> findById(Long id);
    Optional <AppUserEntity> findByUsernameIgnoreCase(String username);
    Optional<AppUserEntity> findByEmailIgnoreCase(String email);
    @EntityGraph(attributePaths = {"role"}) 
    @Query("SELECT u FROM AppUserEntity u WHERE " +
            "UPPER(u.username) = UPPER(:input) OR " +
            "UPPER(u.email) = UPPER(:input) OR " +
            "CAST(u.id AS string) = :input")
    Optional<AppUserEntity> findByIdentifier(@Param("input") String input);


    @Query("SELECT u FROM AppUserEntity u WHERE u.id = :id AND u.isActive = true AND u.isLocked = false ")
    Optional<AppUserEntity> findByIdAndIsActiveTrueAndIsLockedFalse(Long id);

    @Query("SELECT u FROM AppUserEntity u WHERE u.id IN :ids AND u.isActive = true AND u.isLocked = false")
    List<AppUserEntity> findAllByIdInAndIsActiveTrueAndIsLockedFalse(@Param("ids") List<Long> ids);


    Page<AppUserEntity> findAll(Pageable pageable);
    @Query("SELECT u FROM AppUserEntity u WHERE UPPER(u.username)= UPPER(:username) OR UPPER(u.email)= UPPER(:email) ")

    Optional<AppUserEntity> findByUsernameIgnoreCaseOrEmailIgnoreCase(@Param("username") String username, @Param("email") String email);
    @Query("SELECT u.id FROM AppUserEntity u WHERE LOWER(u.username) = LOWER(:username) ")
    Optional<Long> findIdByUsername(@Param("username") String username);


    // Использование проекционного конструктора DTO в JPQL с агрегатными подзапросами вычисляет счетчики и состояния связей в один roundtrip к БД.
      @Query("""
    SELECT new project.interactivenovelplatform.dto.response.ProfileResponseDto(
        u.id,
        u.username,
        u.avatarUrl,
        u.email,
        u.registrationDate,
        u.isActive,
        
        (SELECT COUNT(n) FROM NovelEntity n
         WHERE n.author = u
         AND n.status IN :activeStatuses),
        
        (SELECT COUNT(f1) FROM UserFollowerEntity f1 WHERE f1.receiver = u),
        
        (SELECT COUNT(f2) FROM UserFollowerEntity f2 WHERE f2.sender = u),
        
        (SELECT COUNT(fr) FROM UserFriendEntity fr
         WHERE fr.status = :friendStatus
         AND (fr.sender = u OR fr.receiver = u)),
         
        (SELECT COUNT(cf) FROM UserCloseFriendsEntity cf WHERE cf.owner = u),
        
        false,
        
        CASE WHEN :currentUserId IS NULL THEN false
             ELSE ((SELECT COUNT(f3) FROM UserFollowerEntity f3 WHERE f3.sender.id = :currentUserId AND f3.receiver = u) > 0)
        END,
        
        CASE WHEN :currentUserId IS NULL THEN false
             ELSE ((SELECT COUNT(fr2) FROM UserFriendEntity fr2
                    WHERE fr2.status = :friendStatus
                    AND ((fr2.sender.id = :currentUserId AND fr2.receiver = u)
                      OR (fr2.sender = u AND fr2.receiver.id = :currentUserId))) > 0)
        END,
        
        CASE WHEN :currentUserId IS NULL THEN false
             ELSE ((SELECT COUNT(cf2) FROM UserCloseFriendsEntity cf2 WHERE cf2.owner.id = :currentUserId AND cf2.friend = u) > 0)
        END,
        
        CASE WHEN :currentUserId IS NULL THEN false
             ELSE ((SELECT COUNT(b1) FROM UserBlockEntity b1 WHERE b1.blocker.id = :currentUserId AND b1.blocked = u) > 0)
        END,
        
        CASE WHEN :currentUserId IS NULL THEN false
             ELSE ((SELECT COUNT(b2) FROM UserBlockEntity b2 WHERE b2.blocker = u AND b2.blocked.id = :currentUserId) > 0)
        END
    )
    FROM AppUserEntity u
    WHERE u.id = :targetUserId
""")
    Optional<ProfileResponseDto> getFullProfileWithEnums(
            @Param("targetUserId") Long targetUserId,
            @Param("currentUserId") Long currentUserId,
            @Param("friendStatus") RelationStatus friendStatus,
            @Param("activeStatuses") List<Novel> activeStatuses
    );

    default Optional<ProfileResponseDto> getFullProfile(Long targetUserId, Long currentUserId) {
        return getFullProfileWithEnums(
                targetUserId,
                currentUserId,
                RelationStatus.FRIEND,
                List.of(Novel.COMPLETED, Novel.IN_PROGRESS, Novel.HIATUS)
        );
    }

    @Query("""
    SELECT new project.interactivenovelplatform.dto.response.RelationshipStateDto(
        u.id,
        ((SELECT COUNT(f1) FROM UserFollowerEntity f1\s
                  WHERE f1.sender.id = :currentUserId AND f1.receiver.id = :targetUserId) > 0),
        
        ((SELECT COUNT(f2) FROM UserFollowerEntity f2\s
          WHERE f2.sender.id = :targetUserId AND f2.receiver.id = :currentUserId) > 0),
          
        ((SELECT COUNT(fr) FROM UserFriendEntity fr
          WHERE fr.status = :friendStatus
          AND ((fr.sender.id = :currentUserId AND fr.receiver.id = :targetUserId)
            OR (fr.sender.id = :targetUserId AND fr.receiver.id = :currentUserId))) > 0),
            
        ((SELECT COUNT(cf) FROM UserCloseFriendsEntity cf WHERE cf.owner.id = :currentUserId AND cf.friend.id = :targetUserId) > 0),
        
        ((SELECT COUNT(b1) FROM UserBlockEntity b1 WHERE b1.blocker.id = :currentUserId AND b1.blocked.id = :targetUserId) > 0),
        
        ((SELECT COUNT(b2) FROM UserBlockEntity b2 WHERE b2.blocker.id = :targetUserId AND b2.blocked.id = :currentUserId) > 0)
    )
    FROM AppUserEntity u WHERE u.id = :targetUserId
""")
    Optional<RelationshipStateDto> getRelationshipStateWithEnums(
            @Param("currentUserId") Long currentUserId,
            @Param("targetUserId") Long targetUserId,
            @Param("friendStatus") RelationStatus friendStatus
    );

    default Optional<RelationshipStateDto> getRelationshipState(Long currentUserId, Long targetUserId) {
        return getRelationshipStateWithEnums(currentUserId, targetUserId, RelationStatus.FRIEND);
    }

    @Query("""
    SELECT new project.interactivenovelplatform.dto.response.RelationshipStateDto(
        u.id,
        ((SELECT COUNT(f1) FROM UserFollowerEntity f1
                  WHERE f1.sender.id = :currentUserId AND f1.receiver.id = u.id) > 0),

        ((SELECT COUNT(f2) FROM UserFollowerEntity f2
          WHERE f2.sender.id = u.id AND f2.receiver.id = :currentUserId) > 0),

        ((SELECT COUNT(fr) FROM UserFriendEntity fr
          WHERE fr.status = :friendStatus
          AND ((fr.sender.id = :currentUserId AND fr.receiver.id = u.id)
            OR (fr.sender.id = u.id AND fr.receiver.id = :currentUserId))) > 0),

        ((SELECT COUNT(cf) FROM UserCloseFriendsEntity cf WHERE cf.owner.id = :currentUserId AND cf.friend.id = u.id) > 0),

        ((SELECT COUNT(b1) FROM UserBlockEntity b1 WHERE b1.blocker.id = :currentUserId AND b1.blocked.id = u.id) > 0),

        ((SELECT COUNT(b2) FROM UserBlockEntity b2 WHERE b2.blocker.id = u.id AND b2.blocked.id = :currentUserId) > 0)
    )
    FROM AppUserEntity u WHERE u.id IN :targetIds
""")
    List<RelationshipStateDto> findAllRelationshipStatesWithEnums(
            @Param("currentUserId") Long currentUserId, 
            @Param("targetIds") List<Long> targetIds,
            @Param("friendStatus") RelationStatus friendStatus
    );

    default List<RelationshipStateDto> findAllRelationshipStates(Long currentUserId, List<Long> targetIds) {
        return findAllRelationshipStatesWithEnums(currentUserId, targetIds, RelationStatus.FRIEND);
    }


    @Modifying(clearAutomatically = true)
    @Query("UPDATE AppUserEntity u SET u.isActive = true WHERE u.id = :userId")
    void activateUser(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AppUserEntity u SET u.email = :email WHERE u.id = :userId")
    void updateUserEmail(@Param("userId") Long userId, @Param("email") String email);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AppUserEntity u SET u.passwordHash = :password WHERE u.id = :userId")
    void updateUserPassword(@Param("userId") Long userId, @Param("password") String password);


    boolean existsByEmail(@NotBlank(message = "Email не может быть пустым.") @Email(message = "Некорректный формат email адреса.") String email);
}
