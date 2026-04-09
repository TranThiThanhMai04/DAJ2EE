package DAJ2EE.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * File cấu hình riêng biệt để kích hoạt Method-Level Security.
 * Điều này cho phép sử dụng @PreAuthorize, @PostAuthorize, @Secured,...
 * mà không cần sửa vào file SecurityConfig chung của nhóm.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {
    // Chỉ cần khai báo @EnableMethodSecurity là đủ để Spring quét các Annotation bảo mật.
}
