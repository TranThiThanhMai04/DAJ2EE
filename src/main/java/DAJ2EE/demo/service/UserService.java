package DAJ2EE.demo.service;

import DAJ2EE.demo.dto.UserRegistrationDto;

/**
 * Interface định nghĩa các hành động liên quan tới User
 */
public interface UserService {

    /**
     * Đăng ký tài khoản mới cho Tenant (Cư dân).
     * Ném IllegalArgumentException nếu username (SĐT) đã tồn tại.
     */
    void registerTenant(UserRegistrationDto registrationDto);

    /**
     * Kiểm tra xem username (SĐT) đã tồn tại trong hệ thống chưa
     */
    boolean isUsernameExist(String username);
}
