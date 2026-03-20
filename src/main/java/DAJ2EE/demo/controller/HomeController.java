package DAJ2EE.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        // Trả về file index.html trong thư mục templates
        return "index";
    }

    @GetMapping("/admin")
    public String adminIndex(Model model) {
        // Mock data for demo
        model.addAttribute("fullName", "Admin A");
        // Trả về file admin/index.html
        return "admin/index";
    }

    @GetMapping("/tenant")
    public String tenantIndex(Model model) {
        // Mock data for demo
        model.addAttribute("fullName", "Nguyễn Văn A");
        model.addAttribute("totalUnpaid", "1.200.000 đ");
        // Trả về file tenant/index.html
        return "tenant/index";
    }
}
