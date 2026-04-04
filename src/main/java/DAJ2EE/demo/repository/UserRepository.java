package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
    // Tìm kiếm bằng username (SĐT) HOẶC email để hỗ trợ đăng nhập linh hoạt
    Optional<User> findByUsernameOrEmail(String username, String email);

    // Lấy danh sách toàn bộ người dùng thuộc 1 vai trò nhất định
    List<User> findByRoleName(String roleName);

    // Đếm số lượng người dùng theo tên Vai trò
    long countByRoleName(String roleName);

    // Đếm số lượng người dùng theo tên Vai trò và trạng thái (Active = 1)
    long countByRoleNameAndStatus(String roleName, int status);

    // Lấy danh sách người dùng chưa được duyệt (enabled = false)
    List<User> findByEnabledFalse();

    // Lấy danh sách người dùng đã được duyệt (enabled = true)
    List<User> findByEnabledTrue();

    // Kiểm tra xem username (SĐT) đã tồn tại cho người dùng khác hay chưa
    boolean existsByUsernameAndIdNot(String username, Long id);
}
