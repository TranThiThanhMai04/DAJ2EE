package DAJ2EE.demo.controller;

import DAJ2EE.demo.repository.RoomRepository;
import DAJ2EE.demo.entity.RoomStatus;
import DAJ2EE.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final RoomRepository roomRepository;

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        return "admin/dashboard"; // Thống kê (Liêu)
    }

    @GetMapping("/reports")
    public String showReports(Model model) {
        model.addAttribute("rooms", roomRepository.findByStatus(RoomStatus.OCCUPIED));
        return "admin/reports"; // Báo cáo (Liêu)
    }

    @PostMapping("/notifications/send-all")
    public ResponseEntity<?> sendBulkNotification(@RequestBody Map<String, String> payload) {
        String title = payload.get("title");
        String content = payload.get("content");
        
        if (title != null && content != null) {
            notificationService.sendNotificationToAll(title, content);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("Dữ liệu không hợp lệ");
    }

    @PostMapping("/notifications/send-to-room")
    public ResponseEntity<?> sendRoomNotification(@RequestBody Map<String, Object> payload) {
        String title = (String) payload.get("title");
        String content = (String) payload.get("content");
        Object roomIdObj = payload.get("roomId");
        
        if (title != null && content != null && roomIdObj != null) {
            Long roomId = Long.valueOf(roomIdObj.toString());
            notificationService.sendNotificationToRoom(roomId, title, content);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("Dữ liệu không hợp lệ");
    }
}
