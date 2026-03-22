package DAJ2EE.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(columnNames = "room_number"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Mã phòng không được để trống")
    @Column(name = "room_number", nullable = false, length = 10, unique = true)
    private String roomNumber;

    @NotNull(message = "Giá thuê không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá thuê phải lớn hơn hoặc bằng 0")
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "monthly_rent", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRentCompat;

    @DecimalMin(value = "0.0", inclusive = false, message = "Diện tích phải lớn hơn 0")
    @Column(name = "area")
    private BigDecimal areaM2;

    @NotNull(message = "Trạng thái phòng không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status = RoomStatus.EMPTY;

    @Column(name = "description", columnDefinition = "TEXT")
    private String note;

    @PrePersist
    @PreUpdate
    private void syncLegacyColumns() {
        this.monthlyRentCompat = this.monthlyRent;
    }
}
