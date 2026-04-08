package DAJ2EE.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static DAJ2EE.demo.security.TwoFactorAuthenticationSuccessHandler.SESSION_IS_2FA_REQUIRED;
import static DAJ2EE.demo.security.TwoFactorAuthenticationSuccessHandler.SESSION_IS_2FA_VERIFIED;

@Component
public class TwoFactorEnforcementFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Bỏ qua các endpoint phục vụ flow 2FA để tránh redirect loop
        if (path.equals("/auth/verify-2fa") || path.startsWith("/2fa/") || path.startsWith("/logout")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Chỉ chặn các khu vực cần 2FA
        if (!(path.equals("/admin") || path.startsWith("/admin/") || path.equals("/tenant") || path.startsWith("/tenant/"))) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        boolean required = session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_IS_2FA_REQUIRED));
        boolean verified = session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_IS_2FA_VERIFIED));

        if (required && !verified) {
            response.sendRedirect("/auth/verify-2fa");
            return;
        }

        filterChain.doFilter(request, response);
    }
}

