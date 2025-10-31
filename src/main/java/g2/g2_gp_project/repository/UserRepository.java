package g2.g2_gp_project.repository;

import g2.g2_gp_project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User>  findByUsername(String username);
}
