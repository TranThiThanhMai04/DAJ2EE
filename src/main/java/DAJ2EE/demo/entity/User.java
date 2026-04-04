package DAJ2EE.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;
    private String password;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "hometown")
    private String hometown;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String cccd; // Số căn cước công dân

    @Column(name = "provider")
    private String provider; // LOCAL / GOOGLE

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    private int status;
    private boolean enabled = false;
}
