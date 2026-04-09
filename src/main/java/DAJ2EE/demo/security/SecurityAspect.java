package DAJ2EE.demo.security;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * AOP Security Aspect: Một "màng lọc" vô hình bảo vệ toàn bộ Controller.
 * Cách này cực kỳ mạnh mẽ vì nó không cần @PreAuthorize trên từng Method,
 * cũng không cần sửa file SecurityConfig chung.
 * Nó tự động kiểm tra quyền Admin cho mọi request vào các Controller của Admin.
 */
@Aspect
@Component
public class SecurityAspect {

    // Điểm cắt (Pointcut): Chặn mọi method bên trong AdminController hoặc bất kỳ controller nào thuộc package admin.
    @Before("execution(* DAJ2EE.demo.controller.AdminController.*(..)) || " +
            "execution(* DAJ2EE.demo.controller.InvoiceController.*(..)) || " +
            "execution(* DAJ2EE.demo.controller.ContractController.*(..))")
    public void checkAdminRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Bạn cần đăng nhập để truy cập!");
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(grant -> grant.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            // Ném lỗi 403 để GlobalSecurityExceptionHandler bắt và hiển thị trang 403 Glassmorphism
            throw new AccessDeniedException("Chỉ dành cho Quản trị viên!");
        }
    }
}
