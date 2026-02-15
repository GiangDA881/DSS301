package g2.g2_gp_project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_sequences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderSequence {
    @Id
    @Column(length = 50)
    private String name;

    @Column(name = "last_value")
    private Long lastValue;
}
