package DAJ2EE.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    // Đường dẫn đăng nhập cho người dùng (Cư dân)
    @GetMapping("/login")
    public String userLogin() {
        return "login"; // Trả về templates/login.html
    }

    // Đường dẫn đăng ký cho người dùng (Cư dân)
    @GetMapping("/register")
    public String userRegister() {
        return "register"; // Trả về templates/register.html
    }

    // Đường dẫn đăng nhập cho Admin (Chủ nhà)
    @GetMapping("/admin/login")
    public String adminLogin() {
        return "admin/login"; // Trả về templates/admin/login.html
    }
}
