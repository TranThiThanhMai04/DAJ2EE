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
     * Phê duyệt tài khoản khách thuê (Set status = 1)
     */
    void approveTenant(Long id);

    /**
     * Kiểm tra xem username (SĐT) đã tồn tại trong hệ thống chưa
     */
    boolean isUsernameExist(String username);
    java.util.Optional<DAJ2EE.demo.entity.User> findByUsername(String username);

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
     * Lấy toàn bộ danh sách người dùng trong hệ thống
     */
    java.util.List<DAJ2EE.demo.entity.User> getAllUsers();

    /**
     * Lấy danh sách những người dùng có vai trò là Khách thuê (ROLE_TENANT)
     */
    java.util.List<DAJ2EE.demo.entity.User> getAllTenants();

    /**
     * Lấy Khách thuê theo ID
     */
    DAJ2EE.demo.entity.User getTenantById(Long id);

    /**
     * Cập nhật thông tin khách thuê
     */
    void updateTenant(Long id, DAJ2EE.demo.dto.TenantRequestDto dto);

    /**
     * Xóa khách thuê (Chỉ cho phép khi không có hợp đồng đang active)
     */
    void deleteTenant(Long id);
}
