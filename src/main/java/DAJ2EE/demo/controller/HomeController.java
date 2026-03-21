package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.Notification;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.repository.NotificationRepository;
import DAJ2EE.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

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
    public String tenantIndex(Model model, Authentication auth) {
        if (auth == null) return "redirect:/login";
        
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        model.addAttribute("fullName", user != null ? user.getFullName() : auth.getName());
        model.addAttribute("totalUnpaid", "1.200.000 đ");
        
        if (user != null) {
            List<Notification> unreadNotifs = notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(n -> n.getTenant() != null && n.getTenant().getId().equals(user.getId()) && !n.isRead())
                .toList();
            model.addAttribute("unreadNotifs", unreadNotifs);
        }
        
        // Trả về file tenant/index.html
        return "tenant/index";
    }
}
