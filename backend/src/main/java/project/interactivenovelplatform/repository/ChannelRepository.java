package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.ChannelEntity;

@Repository
public interface ChannelRepository extends JpaRepository<ChannelEntity, Long> {

}
