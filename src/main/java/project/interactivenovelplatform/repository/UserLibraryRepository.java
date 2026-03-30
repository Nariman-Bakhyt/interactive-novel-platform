package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.interactivenovelplatform.entity.UserLibraryEntity;

public interface UserLibraryRepository extends JpaRepository<UserLibraryEntity, Long> {
}
