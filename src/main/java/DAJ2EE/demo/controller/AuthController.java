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

    
    @GetMapping("/login")
    public String userLogin() {
        return "login"; // templates/login.html
    }

    
    @GetMapping("/admin/login")
    public String adminLogin() {
        return "admin/login"; 
    }

    
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
      
        model.addAttribute("registerForm", new UserRegistrationDto());
        return "register"; // templates/register.html
    }

    
    @PostMapping("/register")
    public String handleRegister(
            @Valid @ModelAttribute("registerForm") UserRegistrationDto dto,
            BindingResult bindingResult,
            Model model) {

        
        if (bindingResult.hasErrors()) {
            return "register";
        }

       
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.registerForm", "Mật khẩu xác nhận không khớp");
            return "register";
        }

        
        if (userService.isUsernameExist(dto.getPhone())) {
            bindingResult.rejectValue("phone", "error.registerForm", "Số điện thoại này đã được đăng ký!");
            return "register";
        }

        
        if (userService.isEmailExist(dto.getEmail())) {
            bindingResult.rejectValue("email", "error.registerForm", "Email này đã tồn tại trong hệ thống!");
            return "register";
        }

        
        try {
            userService.registerTenant(dto);
            model.addAttribute("successMessage", "Đăng ký thành công! Vui lòng chờ Ban Quản Lý phê duyệt tài khoản.");
            model.addAttribute("registerForm", new UserRegistrationDto()); // Reset form
            return "register";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Đã có lỗi xảy ra trong quá trình đăng ký. Vui lòng thử lại!");
            return "register";
        }
    }
}
