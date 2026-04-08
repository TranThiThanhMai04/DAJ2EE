package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.UserOTP;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOTPRepository extends JpaRepository<UserOTP, Long> {
}

