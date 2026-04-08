package DAJ2EE.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "service_usage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "old_value", nullable = false)
    private Integer oldValue = 0;

    @Column(name = "new_value", nullable = false)
    private Integer newValue = 0;

    @Column(name = "previous_reading", nullable = false)
    private Integer previousReading = 0;

    @Column(name = "current_reading", nullable = false)
    private Integer currentReading = 0;

    @Column(name = "reading_date", nullable = false)
    private LocalDate readingDate;

    @Column(name = "reading_year", nullable = false)
    private Integer readingYear;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "year", nullable = false)
    private Integer year;
}
