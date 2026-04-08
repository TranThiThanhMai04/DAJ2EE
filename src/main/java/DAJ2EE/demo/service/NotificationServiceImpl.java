package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.Notification;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.repository.NotificationRepository;
import DAJ2EE.demo.repository.UserRepository;
import DAJ2EE.demo.repository.ContractRepository;
import DAJ2EE.demo.entity.Contract;
import DAJ2EE.demo.entity.ContractStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ContractRepository contractRepository;

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
    @Transactional
    public void sendNotificationToUser(Long userId, String title, String content) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            Notification notification = Notification.builder()
                    .title(title)
                    .content(content)
                    .createdAt(LocalDateTime.now())
                    .user(user)
                    .isRead(false)
                    .build();
            notificationRepository.save(notification);
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
        });
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Override
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
}
