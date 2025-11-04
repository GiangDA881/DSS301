package g2.g2_gp_project.repository;

import g2.g2_gp_project.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    Optional<Customer> findByCustomerId(String customerId);
    List<Customer> findByCountry(String country);
    List<Customer> findByRegion(String region);

    @Query("SELECT DISTINCT c.country FROM Customer c ORDER BY c.country")
    List<String> findAllCountries();
}
