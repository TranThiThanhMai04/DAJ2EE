package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    
    // Tìm kiếm bằng username (SĐT) HOẶC email để hỗ trợ đăng nhập linh hoạt
    Optional<User> findByUsernameOrEmail(String username, String email);
}
