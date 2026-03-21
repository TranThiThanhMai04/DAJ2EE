package DAJ2EE.demo.service;

import DAJ2EE.demo.dto.UserRegistrationDto;
import DAJ2EE.demo.entity.Role;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.repository.RoleRepository;
import DAJ2EE.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

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
        user.setUsername(dto.getPhone());     // SĐT làm username
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
        user.setStatus(1); // 1 = Active (đang hoạt động)

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
    public boolean isEmailExist(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}