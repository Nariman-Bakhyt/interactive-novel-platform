package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.interactivenovelplatform.entity.GenreEntity;
import project.interactivenovelplatform.entity.TagEntity;

import java.util.Collection;
import java.util.List;

public interface GenreRepository extends JpaRepository<GenreEntity,Long> {
    List<GenreEntity> findAllByNameInIgnoreCase(Collection<String> names);
}
