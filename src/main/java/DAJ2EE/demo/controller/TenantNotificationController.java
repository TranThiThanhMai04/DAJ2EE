package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.service.CurrentUserService;
import DAJ2EE.demo.service.NotificationService;
import DAJ2EE.demo.service.NotificationRealtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
@RequestMapping("/tenant/notifications")
@RequiredArgsConstructor
public class TenantNotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;
    private final NotificationRealtimeService notificationRealtimeService;

    @GetMapping
    public String listNotifications(Model model, Authentication authentication) {
        User user = currentUserService.getRequiredUser(authentication);
        Long userId = user.getId();
        model.addAttribute("notifications", notificationService.getNotificationsForUser(userId));
        model.addAttribute("unreadCount", notificationService.countUnread(userId));
        return "tenant/notifications";
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter streamNotifications(Authentication authentication) {
        User user = currentUserService.getRequiredUser(authentication);
        return notificationRealtimeService.subscribe(user.getId());
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
        User user = currentUserService.getRequiredUser(authentication);
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok().build();
    }
}
