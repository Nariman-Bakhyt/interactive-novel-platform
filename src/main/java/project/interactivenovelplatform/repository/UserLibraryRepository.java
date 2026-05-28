package project.interactivenovelplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.dto.response.UserLibraryStatusDto;
import project.interactivenovelplatform.entity.PrivacyLevel;
import project.interactivenovelplatform.entity.UserLibraryEntity;
import project.interactivenovelplatform.entity.UserNovelId;

import java.util.List;
import java.util.Optional;
@Repository
public interface UserLibraryRepository extends JpaRepository<UserLibraryEntity, UserNovelId> {
    @EntityGraph(attributePaths = {"novel"})
    Optional<UserLibraryEntity> findById(UserNovelId id);

    @EntityGraph(attributePaths = {"novel"})
    Page<UserLibraryEntity> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"novel"})
    Page<UserLibraryEntity> findByUserIdAndPrivacyLevelIn(Long userId, List<PrivacyLevel> levels, Pageable pageable);

    @Query("SELECT new project.interactivenovelplatform.dto.response.UserLibraryStatusDto(ul.novel.id, ul.status) " +
            "FROM UserLibraryEntity ul WHERE ul.user.id = :userId")
    List<UserLibraryStatusDto> findAllStatusesByUserId(@Param("userId") Long userId);
}
