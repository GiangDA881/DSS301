package g2.g2_gp_project.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import g2.g2_gp_project.entity.Product;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    Optional<Product> findByStockCode(String stockCode);
}
