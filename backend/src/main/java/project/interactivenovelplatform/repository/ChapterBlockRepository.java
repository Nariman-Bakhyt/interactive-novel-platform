package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.ChapterBlockEntity;

@Repository
public interface ChapterBlockRepository extends JpaRepository<ChapterBlockEntity, Long> {

}
