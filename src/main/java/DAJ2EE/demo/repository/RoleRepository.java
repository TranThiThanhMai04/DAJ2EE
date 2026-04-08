package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    // Tìm role theo tên (ví dụ: "ROLE_TENANT", "ROLE_ADMIN")
    Role findByName(String name);
}
