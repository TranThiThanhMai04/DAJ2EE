package DAJ2EE.demo.security;

import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.entity.UserOTP;
import DAJ2EE.demo.repository.UserOTPRepository;
import DAJ2EE.demo.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class TwoFactorAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    public static final String SESSION_IS_2FA_REQUIRED = "is2faRequired";
    public static final String SESSION_IS_2FA_VERIFIED = "is2faVerified";
    public static final String SESSION_POST_LOGIN_REDIRECT = "postLoginRedirect";

    private final UserRepository userRepository;
    private final UserOTPRepository userOTPRepository;

    public TwoFactorAuthenticationSuccessHandler(UserRepository userRepository, UserOTPRepository userOTPRepository) {
        this.userRepository = userRepository;
        this.userOTPRepository = userOTPRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        boolean isAdmin = hasAuthority(authentication, "ROLE_ADMIN");
        boolean isTenant = hasAuthority(authentication, "ROLE_TENANT");

        String identifier = resolveIdentifier(authentication);
        Optional<User> userOpt = (identifier == null)
                ? Optional.empty()
                : userRepository.findByUsernameOrEmail(identifier, identifier);

        HttpSession session = request.getSession(true);
        String redirect = isAdmin ? "/admin" : "/tenant";
        session.setAttribute(SESSION_POST_LOGIN_REDIRECT, redirect);

        if (userOpt.isEmpty()) {
            // Không xác định được user -> giữ nguyên behavior an toàn
            session.setAttribute(SESSION_IS_2FA_REQUIRED, false);
            session.setAttribute(SESSION_IS_2FA_VERIFIED, true);
            response.sendRedirect(redirect);
            return;
        }

        User user = userOpt.get();
        UserOTP otp = userOTPRepository.findById(user.getId()).orElse(null);

        boolean is2faEnabled = otp != null && Boolean.TRUE.equals(otp.getIs2faEnabled());
        boolean mustVerify2fa = isAdmin || (isTenant && is2faEnabled);

        session.setAttribute(SESSION_IS_2FA_REQUIRED, mustVerify2fa);
        session.setAttribute(SESSION_IS_2FA_VERIFIED, !mustVerify2fa);

        if (!mustVerify2fa) {
            response.sendRedirect(redirect);
            return;
        }

        // Nếu bắt buộc 2FA nhưng chưa setup secret, điều hướng thẳng tới setup để tránh dead-end.
        if (otp == null || otp.getSecretKey() == null || otp.getSecretKey().isBlank()) {
            response.sendRedirect("/2fa/setup");
            return;
        }

        response.sendRedirect("/auth/verify-2fa");
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            if (authority.equals(granted.getAuthority())) return true;
        }
        return false;
    }

    private String resolveIdentifier(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oAuth2User) {
            Object email = oAuth2User.getAttributes().get("email");
            return email != null ? String.valueOf(email) : authentication.getName();
        }
        return authentication.getName();
    }
}

