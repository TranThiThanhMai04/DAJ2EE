package DAJ2EE.demo.controller;

import DAJ2EE.demo.dto.ProfileUpdateDto;
import DAJ2EE.demo.dto.ChangePasswordDto;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.repository.UserRepository;
import DAJ2EE.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller xử lý các trang và API liên quan đến Hồ sơ cá nhân.
 */
@Controller
public class ProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    // Removed duplicate GET /tenant/profile to avoid conflict with TenantUserController.
    // Use TenantUserController#viewProfile for the UI page.

    /**
     * [POST] /api/profile/update: API cập nhật hồ sơ cá nhân.
     * Sử dụng ProfileUpdateDto để tránh Mass Assignment.
     */
    @PostMapping("/api/profile/update")
    @ResponseBody
    public ResponseEntity<?> updateProfile(@Valid @RequestBody ProfileUpdateDto dto, 
                                          BindingResult bindingResult,
                                          Authentication authentication) {
        
        // 1. Kiểm tra lỗi Validation (@NotBlank)
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            // 2. Lấy định danh hiện tại (username cho form login, email cho OAuth2)
            String currentUsername;
            if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oAuth2User) {
                currentUsername = oAuth2User.getAttribute("email");
            } else {
                currentUsername = authentication.getName();
            }
            
            // 3. Gọi service để thực hiện logic cập nhật
            userService.updateProfile(currentUsername, dto);

            // 4. Trả về thông báo thành công
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Cập nhật hồ sơ thành công!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Có lỗi xảy ra trong quá trình cập nhật: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * API Đổi mật khẩu
     */
    @PostMapping("/api/profile/change-password")
    @ResponseBody
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordDto dto,
                                           BindingResult bindingResult,
                                           Authentication authentication) {
        
        // 1. Kiểm tra lỗi Validation (@NotBlank)
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            // 2. Lấy username hiện tại
            String currentUsername = authentication.getName();

            // 3. Gọi service xử lý logic đổi mật khẩu
            userService.changePassword(currentUsername, dto);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Đổi mật khẩu thành công!");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
