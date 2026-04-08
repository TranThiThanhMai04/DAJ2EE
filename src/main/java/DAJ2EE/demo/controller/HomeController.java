package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.repository.UserRepository;
import DAJ2EE.demo.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/tenant")
    public String tenantIndex(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier;
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oAuth2User) {
            identifier = oAuth2User.getAttribute("email");
        } else {
            identifier = auth != null ? auth.getName() : null;
        }

        if (identifier != null) {
            userRepository.findByUsernameOrEmail(identifier, identifier).ifPresent(u -> {
                model.addAttribute("fullName", u.getFullName());
                // Load thông báo thực từ database
                model.addAttribute("notifications", notificationService.getNotificationsForUser(u.getId()));
                model.addAttribute("unreadCount", notificationService.countUnread(u.getId()));
            });
        }

        // Fallback nếu không load được
        if (!model.containsAttribute("notifications")) {
            model.addAttribute("notifications", java.util.Collections.emptyList());
        }

        model.addAttribute("totalUnpaid", "1.200.000 đ");
        return "tenant/index";
    }
}
