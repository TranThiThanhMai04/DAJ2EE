package DAJ2EE.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "repair_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RepairRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private User tenant;

    @Column(nullable = false)
    private String roomNumber; // Thêm phòng

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private String priority; // LOW, MEDIUM, HIGH

    private String imageUrl; // Server path to the image

    // PENDING, IN_PROGRESS, COMPLETED, REJECTED
    @Column(nullable = false)
    private String status;

    private LocalDateTime createdAt;
    
    private String adminReply; // Lời nhắn từ admin khi cập nhật trạng thái

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }
}
