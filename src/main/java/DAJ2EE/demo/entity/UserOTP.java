package DAJ2EE.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_2fa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserOTP {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "secret_key", nullable = false, length = 128)
    private String secretKey;

    @Column(name = "is_2fa_enabled", nullable = false)
    private Boolean is2faEnabled = false;

    @Column(name = "backup_code", length = 64)
    private String backupCode;
}

