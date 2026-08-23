package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.GenreEntity;

import java.util.Collection;
import java.util.List;

@Repository
public interface GenreRepository extends JpaRepository<GenreEntity,Long> {
    List<GenreEntity> findAllByNameInIgnoreCase(Collection<String> names);
}
