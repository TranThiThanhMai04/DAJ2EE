package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.repository.UserRepository;
import DAJ2EE.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @GetMapping("/")
    public String index() {
        // Trả về file index.html trong thư mục templates
        return "index";
    }

    @GetMapping("/tenant")
    public String tenantIndex(Model model, Authentication authentication) {
        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            model.addAttribute("fullName", user.getFullName());
            model.addAttribute("notifications", notificationService.getNotificationsForUser(user.getId()));
            model.addAttribute("unreadCount", notificationService.countUnread(user.getId()));
        }
        // Trả về file tenant/index.html
        return "tenant/index";
    }
}
