package g2.g2_gp_project.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import g2.g2_gp_project.entity.Customer;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    Optional<Customer> findByCustomerName(String customerName);
    Optional<Customer> findByCustomerId(String customerId);
}
