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
            BindingResult bindingResult,
            Model model) {

        // Bước 1: Kiểm tra lỗi validation (@NotBlank, @Email, @Pattern, @Size...)
        if (bindingResult.hasErrors()) {
            return "register";
        }

        // Bước 2: Kiểm tra khớp mật khẩu
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.registerForm", "Mật khẩu xác nhận không khớp");
            return "register";
        }

        // Bước 3: Kiểm tra trùng lặp Số điện thoại (Username)
        if (userService.isUsernameExist(dto.getPhone())) {
            bindingResult.rejectValue("phone", "error.registerForm", "Số điện thoại này đã được đăng ký!");
            return "register";
        }

        // Bước 4: Kiểm tra trùng lặp Email
        if (userService.isEmailExist(dto.getEmail())) {
            bindingResult.rejectValue("email", "error.registerForm", "Email này đã tồn tại trong hệ thống!");
            return "register";
        }

        // Bước 5: Thực hiện đăng ký
        try {
            userService.registerTenant(dto);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Đã có lỗi xảy ra trong quá trình đăng ký. Vui lòng thử lại!");
            return "register";
        }

        return "redirect:/login?success";
    }
}
