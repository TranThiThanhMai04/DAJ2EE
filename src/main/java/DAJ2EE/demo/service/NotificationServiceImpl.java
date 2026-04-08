package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.Notification;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.repository.NotificationRepository;
import DAJ2EE.demo.repository.UserRepository;
import DAJ2EE.demo.repository.ContractRepository;
import DAJ2EE.demo.entity.Contract;
import DAJ2EE.demo.entity.ContractStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ContractRepository contractRepository;
    private final NotificationRealtimeService notificationRealtimeService;

    @Override
    @Transactional
    public void sendNotificationToAll(String title, String content) {
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            // Kiểm tra Role của User
            if (user.getRole() != null && user.getRole().getName().contains("TENANT")) {
                sendNotificationToUser(user.getId(), title, content);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotificationToUser(Long userId, String title, String content) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return;
            }

            Notification notification = Notification.builder()
                    .title(title)
                    .content(content)
                    .createdAt(LocalDateTime.now())
                    .user(user)
                    .isRead(false)
                    .build();
            Notification savedNotification = notificationRepository.save(notification);

            long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
            notificationRealtimeService.publishNotification(userId, toRealtimePayload(savedNotification, unreadCount));
            notificationRealtimeService.publishUnreadCount(userId, unreadCount);
        } catch (Exception ex) {
            // Không ném lỗi để tránh ảnh hưởng các nghiệp vụ chính như tạo hợp đồng/hóa đơn.
            log.error("Failed to persist notification for userId={}: {}", userId, ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional
    public void sendNotificationToRoom(Long roomId, String title, String content) {
        // Tìm hợp đồng còn hiệu lực (ACTIVE hoặc PENDING) cho phòng này để lấy người thuê
        java.util.List<ContractStatus> validStatuses = java.util.Arrays.asList(ContractStatus.ACTIVE, ContractStatus.PENDING);
        List<Contract> contracts = contractRepository.findByRoomIdAndStatusIn(roomId, validStatuses);
        if (!contracts.isEmpty()) {
            User tenant = contracts.get(0).getTenant();
            if (tenant != null) {
                sendNotificationToUser(tenant.getId(), title, content);
            }
        }
    }

    @Override
    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);

            if (n.getUser() != null && n.getUser().getId() != null) {
                long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(n.getUser().getId());
                notificationRealtimeService.publishUnreadCount(n.getUser().getId(), unreadCount);
            }
        });
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
        notificationRealtimeService.publishUnreadCount(userId, 0);
    }

    @Override
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    private Map<String, Object> toRealtimePayload(Notification notification, long unreadCount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", notification.getId());
        payload.put("title", notification.getTitle());
        payload.put("content", notification.getContent());
        payload.put("isRead", notification.isRead());
        payload.put("createdAt", notification.getCreatedAt() != null
                ? notification.getCreatedAt().format(TIME_FORMATTER)
                : "");
        payload.put("unreadCount", unreadCount);
        return payload;
    }
}
