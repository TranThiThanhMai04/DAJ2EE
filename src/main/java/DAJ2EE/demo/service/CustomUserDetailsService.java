package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        // CHỈ cho phép đăng nhập bằng Số điện thoại (username) hoặc Email.
        // Tuyệt đối không tìm theo fullName vì Tên không có tính duy nhất.
        User user = userRepository.findByUsernameOrEmail(input, input)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với SĐT hoặc Email: " + input));

        // Nạp tất cả quyền hạn (Role + Permissions) vào danh sách Authorities
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        // 1. Nạp Vai trò (ví dụ: ROLE_ADMIN)
        authorities.add(new SimpleGrantedAuthority(user.getRole().getName()));
        
        // 2. Nạp các quyền từ Role (ví dụ: OP_EDIT_ROOM)
        if (user.getRole() != null && user.getRole().getPermissions() != null) {
            user.getRole().getPermissions().forEach(permission -> {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            });
        }
        
        // 3. Nạp các quyền cụ thể riêng của User (ví dụ: OP_DELETE_ROOM)
        if (user.getPermissions() != null) {
            user.getPermissions().forEach(permission -> {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            });
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(), // Đọc từ Entity. Cư dân mới = false
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities
        );
    }
}
