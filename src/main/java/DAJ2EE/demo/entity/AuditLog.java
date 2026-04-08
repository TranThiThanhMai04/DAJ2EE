package DAJ2EE.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action; // Ví dụ: "Duyệt cư dân", "Cập nhật quyền"

    @Column(columnDefinition = "TEXT")
    private String details; // Chi tiết: "Đã duyệt cư dân Nguyễn Văn A"

    @Column(nullable = false)
    private String username; // Tên người thực hiện

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "ip_address")
    private String ipAddress;
}
