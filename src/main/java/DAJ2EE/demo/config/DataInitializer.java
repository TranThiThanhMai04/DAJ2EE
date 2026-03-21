package DAJ2EE.demo.config;

import DAJ2EE.demo.entity.Role;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.repository.RoleRepository;
import DAJ2EE.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initAdminUser(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // Đảm bảo roles tồn tại (dùng INSERT IGNORE trong data.sql nên thường đã có rồi)
            List<Role> roles = roleRepository.findAll();
            Role adminRole = roles.stream()
                    .filter(r -> r.getName().equals("ROLE_ADMIN"))
                    .findFirst()
                    .orElse(null);

            if (adminRole == null) {
                System.out.println("⚠️ Không tìm thấy ROLE_ADMIN trong DB. Hãy kiểm tra data.sql.");
                return;
            }

            // Tạo hoặc cập nhật admin với mật khẩu BCrypt đúng
            User admin = userRepository.findByUsername("admin").orElse(new User());
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456")); // Mã hóa bằng PasswordEncoder Bean thực tế
            admin.setFullName("Phạm Thị Ái Ngân");
            admin.setEmail("ngan.admin@gmail.com");
            admin.setRole(adminRole);
            admin.setStatus(1);
            userRepository.save(admin);

            System.out.println("✅ Admin user đã được khởi tạo/cập nhật với mật khẩu BCrypt chuẩn.");
        };
    }
}
