package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.repository.UserRepository;
import DAJ2EE.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@Controller
@RequestMapping("/tenant/notifications")
@RequiredArgsConstructor
public class TenantNotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public String listNotifications(Model model, Authentication authentication) {
        String username = authentication.getName();
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            Long userId = user.get().getId();
            model.addAttribute("notifications", notificationService.getNotificationsForUser(userId));
            model.addAttribute("unreadCount", notificationService.countUnread(userId));
        }
        return "tenant/notifications";
    }

    @PostMapping("/mark-read/{id}")
    @ResponseBody
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mark-all-read")
    @ResponseBody
    public ResponseEntity<?> markAllRead(Authentication authentication) {
        Optional<User> user = userRepository.findByUsername(authentication.getName());
        if (user.isPresent()) {
            notificationService.markAllAsRead(user.get().getId());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }
}
