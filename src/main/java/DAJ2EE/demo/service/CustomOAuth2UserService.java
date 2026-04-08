package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.Role;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.entity.Permission;
import DAJ2EE.demo.repository.RoleRepository;
import DAJ2EE.demo.repository.UserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CustomOAuth2UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(noRollbackFor = DisabledException.class)
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String email = oauth2User.getAttribute("email");
        String fullName = oauth2User.getAttribute("name");
        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google không trả về email hợp lệ.");
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            Role tenantRole = roleRepository.findByName("ROLE_TENANT");
            if (tenantRole == null) {
                throw new IllegalStateException("Không tìm thấy ROLE_TENANT trong hệ thống!");
            }

            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(email);
            newUser.setFullName(fullName);
            newUser.setProvider("GOOGLE");
            newUser.setEnabled(false);
            newUser.setStatus(0);
            newUser.setRole(tenantRole);

            user = userRepository.save(newUser);
        } else {
            String provider = user.getProvider();
            if (provider == null || "LOCAL".equalsIgnoreCase(provider)) {
                user.setProvider("GOOGLE");
                user = userRepository.save(user);
            }
        }

        if (!user.isEnabled()) {
            throw new DisabledException("Tài khoản đang chờ phê duyệt");
        }

        Set<GrantedAuthority> authorities = new HashSet<>();
        if (user.getRole() != null && user.getRole().getName() != null) {
            authorities.add(new SimpleGrantedAuthority(user.getRole().getName()));
        }

        if (user.getRole() != null && user.getRole().getPermissions() != null) {
            for (Permission permission : user.getRole().getPermissions()) {
                if (permission != null && permission.getName() != null) {
                    authorities.add(new SimpleGrantedAuthority(permission.getName()));
                }
            }
        }

        if (user.getPermissions() != null) {
            for (Permission permission : user.getPermissions()) {
                if (permission != null && permission.getName() != null) {
                    authorities.add(new SimpleGrantedAuthority(permission.getName()));
                }
            }
        }

        // Luôn sử dụng email làm định danh duy nhất (username) để đồng nhất với hệ thống local
        return new DefaultOAuth2User(authorities, oauth2User.getAttributes(), "email");
    }
}
