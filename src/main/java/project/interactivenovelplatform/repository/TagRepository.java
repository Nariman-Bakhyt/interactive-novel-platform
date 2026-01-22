package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.interactivenovelplatform.entity.TagEntity;

import java.util.Collection;
import java.util.List;

public interface TagRepository extends JpaRepository<TagEntity, Long> {
    List<TagEntity> findAllByNameInIgnoreCase(Collection<String> names);
}
