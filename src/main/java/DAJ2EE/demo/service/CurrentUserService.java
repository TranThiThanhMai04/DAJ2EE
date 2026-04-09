package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getRequiredUser() {
        return getRequiredUser(SecurityContextHolder.getContext().getAuthentication());
    }

    public User getRequiredUser(Authentication authentication) {
        String identifier = resolveIdentifier(authentication);
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Không tìm thấy người dùng hiện tại");
        }

        return userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng hiện tại"));
    }

    private String resolveIdentifier(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute("email");
            if (email != null && !email.isBlank()) {
                return email;
            }
        }

        if (principal instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();
            if (username != null && !username.isBlank()) {
                return username;
            }
        }

        String fallbackName = authentication.getName();
        if (fallbackName == null || fallbackName.isBlank() || "anonymousUser".equalsIgnoreCase(fallbackName)) {
            return null;
        }

        return fallbackName;
    }
}