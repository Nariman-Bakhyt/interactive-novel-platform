package project.interactivenovelplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.dto.response.ProfileResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUserEntity, Long>, JpaSpecificationExecutor<AppUserEntity> {

    Optional <AppUserEntity> findByUsernameIgnoreCase(String username);
    Optional<AppUserEntity> findByEmailIgnoreCase(String email);
    @EntityGraph(attributePaths = {"role"}) // "roles" должно совпадать с именем поля в Entity
    @Query("SELECT u FROM AppUserEntity u WHERE " +
            "UPPER(u.username) = UPPER(:input) OR " +
            "UPPER(u.email) = UPPER(:input) OR " +
            "CAST(u.id AS string) = :input")
    Optional<AppUserEntity> findByIdentifier(@Param("input") String input);


    @Query("SELECT u FROM AppUserEntity u WHERE u.id = :id AND u.isActive = true AND u.isLocked = false ")
    Optional<AppUserEntity> findByIdAndIsActiveTrueAndIsLockedFalse(Long id);

    Page<AppUserEntity> findAll(Pageable pageable);
    @Query("SELECT u FROM AppUserEntity u WHERE UPPER(u.username)= UPPER(:username) OR UPPER(u.email)= UPPER(:email) ")

    Optional<AppUserEntity> findByUsernameIgnoreCaseOrEmailIgnoreCase(@Param("username") String username, @Param("email") String email);
    @Query("SELECT u.id FROM AppUserEntity u WHERE LOWER(u.username) = LOWER(:username) ")
    Optional<Long> findIdByUsername(@Param("username") String username);


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
         AND n.status IN (
            project.interactivenovelplatform.entity.Novel.COMPLETED,
            project.interactivenovelplatform.entity.Novel.IN_PROGRESS,
            project.interactivenovelplatform.entity.Novel.HIATUS
         )),
        
        (SELECT COUNT(f1) FROM UserFollowerEntity f1 WHERE f1.receiver = u),
        
        (SELECT COUNT(f2) FROM UserFollowerEntity f2 WHERE f2.sender = u),
        
        (SELECT COUNT(fr) FROM UserFriendEntity fr
         WHERE fr.status = project.interactivenovelplatform.entity.RelationStatus.FRIEND
         AND (fr.sender = u OR fr.receiver = u)),
         
        (SELECT COUNT(cf) FROM UserCloseFriendsEntity cf WHERE cf.owner = u),
        
        false,
        
        ((SELECT COUNT(f3) FROM UserFollowerEntity f3 WHERE f3.sender.id = :currentUserId AND f3.receiver = u) > 0),
        
        ((SELECT COUNT(fr2) FROM UserFriendEntity fr2
          WHERE fr2.status = project.interactivenovelplatform.entity.RelationStatus.FRIEND
          AND ((fr2.sender.id = :currentUserId AND fr2.receiver = u)
            OR (fr2.sender = u AND fr2.receiver.id = :currentUserId))) > 0),
        
        ((SELECT COUNT(cf2) FROM UserCloseFriendsEntity cf2 WHERE cf2.owner.id = :currentUserId AND cf2.friend = u) > 0)
    )
    FROM AppUserEntity u
    WHERE u.id = :targetUserId
""")
    Optional<ProfileResponseDto> getFullProfile(
            @Param("targetUserId") Long targetUserId,
            @Param("currentUserId") Long currentUserId
    );
    @Modifying(clearAutomatically = true)
    @Query("UPDATE AppUserEntity u SET u.isActive = true WHERE u.id = :userId")
    void activateUser(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AppUserEntity u SET u.email = :email WHERE u.id = :userId")
    void updateUserEmail(@Param("userId") Long userId, @Param("email") String email);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AppUserEntity u SET u.passwordHash = :password WHERE u.id = :userId")
    void updateUserPassword(@Param("userId") Long userId, @Param("password") String password);

}
