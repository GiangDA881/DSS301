package g2.g2_gp_project.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @Column(name = "customer_id", length = 20)
    private String customerId;

    @Column(name = "customer_name", length = 100, nullable = false)
    private String customerName;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String region;

    @Column(length = 100)
    private String email;

    @Column(length = 10)
    private String gender;

    private Integer age;
}