package DAJ2EE.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name; // e.g., "Electricity", "Water"

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName; // Redundant but required by DB

    @Column(nullable = false, length = 20)
    private String unit; // e.g., "kWh", "m3"

    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal pricePerUnit;

    @Column(name = "price_per_unit", precision = 12, scale = 2, nullable = false)
    private BigDecimal pricePerUnitAlt; // Redundant but required by DB

    @Column(columnDefinition = "TEXT")
    private String description;
}
