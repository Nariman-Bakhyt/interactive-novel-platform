package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.ReadingHistoryEntity;
import project.interactivenovelplatform.entity.UserNovelId;
@Repository
public interface ReadingHistoryRepository extends JpaRepository<ReadingHistoryEntity, UserNovelId> {

}
