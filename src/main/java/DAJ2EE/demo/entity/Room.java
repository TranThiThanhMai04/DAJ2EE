package DAJ2EE.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_number", nullable = false, unique = true)
    private String roomNumber;

    @Column(nullable = false)
    private Double price;

    // Trạng thái: Trống (Available), Đã thuê (Rented), Đang bảo trì (Maintenance)
    @Column(nullable = false)
    private String status = "Available";
}
