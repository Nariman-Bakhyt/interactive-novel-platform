package project.interactivenovelplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.interactivenovelplatform.Entity.AppUser;

public interface UserRepository extends JpaRepository<AppUser, Long> {

}
