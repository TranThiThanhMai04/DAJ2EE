package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    // Tìm kiếm bằng username (SĐT) HOẶC email để hỗ trợ đăng nhập linh hoạt
    Optional<User> findByUsernameOrEmail(String username, String email);

    // Đếm số lượng người dùng theo tên Vai trò
    long countByRoleName(String roleName);

    // Đếm số lượng người dùng theo tên Vai trò và trạng thái (Active = 1)
    long countByRoleNameAndStatus(String roleName, int status);
}
