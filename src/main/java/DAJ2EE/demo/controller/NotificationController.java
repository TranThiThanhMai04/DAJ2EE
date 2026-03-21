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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/admin/notifications")
    public String adminNotifications(Model model, Authentication auth) {
        if(auth != null) {
            model.addAttribute("fullName", auth.getName());
        } else {
            model.addAttribute("fullName", "Admin");
        }
        
        // Find all tenants
        List<User> tenants = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "ROLE_TENANT".equals(u.getRole().getName()))
                .toList();
        
        model.addAttribute("tenants", tenants);
        model.addAttribute("history", notificationRepository.findAllByOrderByCreatedAtDesc());
        return "admin/notifications";
    }

    @PostMapping("/admin/notifications/send")
    public String sendNotification(@RequestParam(required = false) Long tenantId,
                                   @RequestParam String title,
                                   @RequestParam String message,
                                   @RequestParam String type) {
        if (tenantId != null && tenantId > 0) {
            Notification nt = new Notification();
            nt.setTitle(title);
            nt.setMessage(message);
            nt.setType(type);
            nt.setCreatedAt(LocalDateTime.now());
            User tenant = userRepository.findById(tenantId).orElse(null);
            nt.setTenant(tenant);
            notificationRepository.save(nt);
        } else {
            List<User> tenants = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "ROLE_TENANT".equals(u.getRole().getName()))
                .toList();
            for (User t : tenants) {
                Notification nt = new Notification();
                nt.setTitle(title);
                nt.setMessage(message);
                nt.setType(type);
                nt.setCreatedAt(LocalDateTime.now());
                nt.setTenant(t);
                notificationRepository.save(nt);
            }
        }
        
        return "redirect:/admin/notifications?success";
    }

    @GetMapping("/tenant/notifications")
    public String tenantNotifications(Model model, Authentication auth) {
        if (auth == null) return "redirect:/login";
        
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        model.addAttribute("fullName", user != null ? user.getFullName() : auth.getName());
        
        // Only show read notifications in the main history list or all if intended
        // User requirements: "nhấn vào đã đọc rồi thì mới xuất hiện trong thông báo thôi" (Only show in notifications tab AFTER marking as read)
        List<Notification> allNotifs = notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(n -> n.getTenant() != null && user != null && n.getTenant().getId().equals(user.getId()) && n.isRead())
                .toList();
                
        model.addAttribute("notifications", allNotifs);
        return "tenant/notifications";
    }

    @PostMapping("/tenant/notifications/read")
    public String markAsRead(@RequestParam Long id, Authentication auth) {
        if (auth == null) return "redirect:/login";
        Notification nt = notificationRepository.findById(id).orElse(null);
        if (nt != null && nt.getTenant() != null && nt.getTenant().getUsername().equals(auth.getName())) {
            nt.setRead(true);
            notificationRepository.save(nt);
        }
        return "redirect:/tenant/notifications";
    }
}
