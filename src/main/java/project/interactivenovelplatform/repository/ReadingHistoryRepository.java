package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.interactivenovelplatform.entity.ReadingHistoryEntity;
import project.interactivenovelplatform.entity.UserNovelId;

public interface ReadingHistoryRepository extends JpaRepository<ReadingHistoryEntity, UserNovelId> {

}
