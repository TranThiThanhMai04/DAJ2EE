package DAJ2EE.demo.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global Exception Handler dành riêng cho lỗi Access Denied.
 * Khi một Tenant cố tình truy cập vào method có @PreAuthorize("hasRole('ADMIN')"),
 * Spring Security sẽ ném ra AccessDeniedException.
 * ControllerAdvice này sẽ bắt lỗi đó và điều hướng về trang /403.
 */
@ControllerAdvice
public class GlobalSecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDeniedException(AccessDeniedException ex) {
        // Trả về view name '403' tương ứng với templates/403.html
        return "403";
    }
}
