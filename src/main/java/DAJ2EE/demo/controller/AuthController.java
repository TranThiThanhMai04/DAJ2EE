package DAJ2EE.demo.controller;

import DAJ2EE.demo.dto.UserRegistrationDto;
import DAJ2EE.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    // ===== TRANG ĐĂNG NHẬP CƯ DÂN =====
    @GetMapping("/login")
    public String userLogin() {
        return "login"; // templates/login.html
    }

    // ===== TRANG ĐĂNG NHẬP ADMIN =====
    @GetMapping("/admin/login")
    public String adminLogin() {
        return "admin/login"; // templates/admin/login.html
    }

    // ===== HIỂN THỊ FORM ĐĂNG KÝ =====
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        // Gửi một DTO rỗng xuống view để Thymeleaf bind th:object
        model.addAttribute("registerForm", new UserRegistrationDto());
        return "register"; // templates/register.html
    }

    // ===== XỬ LÝ SUBMIT FORM ĐĂNG KÝ =====
    @PostMapping("/register")
    public String handleRegister(
            @Valid @ModelAttribute("registerForm") UserRegistrationDto dto,
            BindingResult bindingResult, // Chứa kết quả validation từ @Valid
            Model model) {

        // Bước 1: Kiểm tra lỗi validation (@NotBlank, @Email, @Size...)
        if (bindingResult.hasErrors()) {
            // Có lỗi → quay lại form, Thymeleaf tự điền thông báo lỗi
            return "register";
        }

        // Bước 2: Kiểm tra mật khẩu và xác nhận mật khẩu có khớp không
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            model.addAttribute("errorMessage", "Mật khẩu và xác nhận mật khẩu không khớp!");
            return "register";
        }

        // Bước 3: Gọi service để đăng ký (service sẽ báo lỗi nếu SĐT trùng)
        try {
            userService.registerTenant(dto);
        } catch (IllegalArgumentException e) {
            // Username (SĐT) đã tồn tại
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }

        // Thành công → chuyển về trang login với thông báo
        return "redirect:/login?success";
    }
}
