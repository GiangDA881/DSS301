package g2.g2_gp_project.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import g2.g2_gp_project.entity.OrderSequence;

public interface OrderSequenceRepository extends JpaRepository<OrderSequence, String> {
}
