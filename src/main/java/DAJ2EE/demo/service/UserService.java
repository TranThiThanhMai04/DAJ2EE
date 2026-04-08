package DAJ2EE.demo.service;

import DAJ2EE.demo.dto.UserRegistrationDto;
import DAJ2EE.demo.dto.ProfileUpdateDto;
import DAJ2EE.demo.dto.ChangePasswordDto;
import DAJ2EE.demo.dto.TenantRequestDto;
import DAJ2EE.demo.entity.User;

import java.util.List;

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
     * Phê duyệt tài khoản khách thuê (Set status = 1)
     */
    void approveTenant(Long id);

    /**
     * Kiểm tra xem username (SĐT) đã tồn tại trong hệ thống chưa
     */
    boolean isUsernameExist(String username);

    /**
     * Kiểm tra xem email đã tồn tại trong hệ thống chưa
     */
    boolean isEmailExist(String email);

    /**
     * Cập nhật Vai trò cho người dùng (Dành cho Admin)
     */
    void updateUserRole(Long targetUserId, Long newRoleId);

    /**
     * Cập nhật Quyền hạn cụ thể cho người dùng (Dành cho Admin)
     */
    void updateUserPermission(Long userId, String permissionName, boolean enabled);

    /**
     * Cập nhật hồ sơ cá nhân cho người dùng hiện tại
     */
    void updateProfile(String currentUsername, ProfileUpdateDto dto);

    /**
     * Đổi mật khẩu cho người dùng hiện tại
     */
    void changePassword(String currentUsername, ChangePasswordDto dto);

    /** Lấy tất cả người dùng */
    List<User> getAllUsers();

    /** Lấy tất cả người dùng có role ROLE_TENANT */
    List<User> getAllTenants();

    /** Lấy thông tin một Tenant theo ID */
    User getTenantById(Long id);

    /** Cập nhật thông tin Tenant (Admin dùng) */
    void updateTenant(Long id, TenantRequestDto dto);

    /** Xóa Tenant khỏi hệ thống */
    void deleteTenant(Long id);

    /** Tìm User theo username */
    User findByUsername(String username);
}
