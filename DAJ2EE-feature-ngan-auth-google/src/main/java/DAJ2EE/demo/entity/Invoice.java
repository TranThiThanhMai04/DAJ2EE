package DAJ2EE.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String roomName;
    private Double totalAmount;
    private Double paidAmount;

    @Column(name = "`month`")
    private String month; // e.g., "03/2026"
    private String status; // "PAID", "UNPAID", "PARTIAL"
    private LocalDate dueDate;

    public Double getRemainingAmount() {
        return totalAmount - (paidAmount != null ? paidAmount : 0.0);
    }
}
