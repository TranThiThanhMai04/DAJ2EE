package DAJ2EE.demo.service;

import DAJ2EE.demo.dto.UserRegistrationDto;
import DAJ2EE.demo.entity.Permission;
import DAJ2EE.demo.entity.Role;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.exception.ResourceNotFoundException;
import DAJ2EE.demo.repository.PermissionRepository;
import DAJ2EE.demo.repository.RoleRepository;
import DAJ2EE.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Đăng ký tài khoản mới cho Tenant.
     * Số điện thoại được dùng làm username.
     */
    @Override
    public void registerTenant(UserRegistrationDto dto) {
        // Kiểm tra username (SĐT) đã tồn tại chưa
        if (isUsernameExist(dto.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại này đã được đăng ký trong hệ thống!");
        }

        // Tạo đối tượng User mới
        User user = new User();
        user.setUsername(dto.getPhone()); // SĐT làm username
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setCccd(dto.getCccd());

        // Mã hóa mật khẩu bằng BCrypt trước khi lưu vào DB
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        // Tìm và gán quyền ROLE_TENANT mặc định
        Role tenantRole = roleRepository.findByName("ROLE_TENANT");
        if (tenantRole == null) {
            throw new IllegalStateException("Không tìm thấy ROLE_TENANT trong hệ thống!");
        }
        user.setRole(tenantRole);
        user.setStatus(1); // Mặc định Hoạt động

        userRepository.save(user);
    }

    /**
     * Kiểm tra xem SĐT đã được đăng ký chưa
     */
    @Override
    public boolean isUsernameExist(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void approveTenant(Long id) {
        User user = getTenantById(id);
        if (user.getStatus() == 1) {
            throw new IllegalArgumentException("Khách thuê này đã được phê duyệt hợp lệ từ trước!");
        }
        user.setStatus(1);
        userRepository.save(user);
    }

    /**
     * Cập nhật Vai trò cho người dùng (Dành cho Admin).
     * Bao gồm các ràng buộc bảo vệ hệ thống và Admin cuối cùng.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void updateUserRole(Long targetUserId, Long newRoleId) {
        // 1. Kiểm tra tồn tại của User mục tiêu
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + targetUserId));

        // 2. Kiểm tra tồn tại của Role mới
        Role newRole = roleRepository.findById(newRoleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Vai trò với ID: " + newRoleId));

        // 3. Lấy thông tin người đang đăng nhập hiện tại
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        // 4. Chống tự giáng cấp (No Self-Demotion)
        if (currentUsername.equals(targetUser.getUsername())) {
            throw new IllegalStateException("Không thể tự thay đổi quyền của chính mình!");
        }

        // 5. Bảo vệ Admin cuối cùng (Last Admin Standing)
        // Nếu User mục tiêu hiện đang là ADMIN và Role mới KHÔNG PHẢI là ADMIN
        if ("ROLE_ADMIN".equals(targetUser.getRole().getName()) && !"ROLE_ADMIN".equals(newRole.getName())) {
            // Đếm số lượng Admin đang active (status = 1)
            long adminCount = userRepository.countByRoleNameAndStatus("ROLE_ADMIN", 1);
            if (adminCount <= 1) {
                throw new IllegalStateException("Hệ thống phải duy trì ít nhất 1 Quản trị viên!");
            }
        }

        // 6. Thực hiện cập nhật
        targetUser.setRole(newRole);
        userRepository.save(targetUser);
    }

    /**
     * Cập nhật Quyền hạn cụ thể cho người dùng (Dành cho Admin)
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void updateUserPermission(Long userId, String permissionName, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));

        Permission permission = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Quyền hạn: " + permissionName));

        if (enabled) {
            user.getPermissions().add(permission);
        } else {
            user.getPermissions().remove(permission);
        }
        userRepository.save(user);
    }

    @Override
    public boolean isEmailExist(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Override
    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public java.util.List<User> getAllTenants() {
        return userRepository.findByRoleName("ROLE_TENANT");
    }

    @Override
    public User getTenantById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách thuê với ID: " + id));
        if (!"ROLE_TENANT".equals(user.getRole().getName())) {
            throw new IllegalArgumentException("Người dùng này không phải là khách thuê");
        }
        return user;
    }

    @Override
    @Transactional
    public void updateTenant(Long id, DAJ2EE.demo.dto.TenantRequestDto dto) {
        User user = getTenantById(id);
        
        // Kiểm tra xem SĐT mới có bị trùng với người khác không
        if (!user.getUsername().equals(dto.getPhone()) && isUsernameExist(dto.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại này đã được đăng ký cho người khác");
        }
        
        user.setUsername(dto.getPhone());
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setCccd(dto.getCccd());
        
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            if (!dto.getPassword().equals(dto.getConfirmPassword())) {
                throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
            }
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        userRepository.save(user);
    }

    @Autowired
    private DAJ2EE.demo.repository.ContractRepository contractRepository;

    @Override
    @Transactional
    public void deleteTenant(Long id) {
        User user = getTenantById(id);
        java.util.List<DAJ2EE.demo.entity.Contract> activeContracts = contractRepository.findByTenantIdAndStatusIn(id, java.util.Arrays.asList(DAJ2EE.demo.entity.ContractStatus.ACTIVE, DAJ2EE.demo.entity.ContractStatus.PENDING));
        if (activeContracts != null && !activeContracts.isEmpty()) {
            throw new IllegalArgumentException("Không thể xóa khách thuê đang có hợp đồng có hiệu lực hoặc đang chờ xác nhận");
        }
        // Xóa tenant
        userRepository.delete(user);
    }
}