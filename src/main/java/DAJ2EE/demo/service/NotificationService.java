package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.Notification;
import java.util.List;

public interface NotificationService {
    void sendNotificationToAll(String title, String content);
    void sendNotificationToUser(Long userId, String title, String content);
    void sendNotificationToRoom(Long roomId, String title, String content);
    List<Notification> getNotificationsForUser(Long userId);
    void markAsRead(Long notificationId);
    void markAllAsRead(Long userId);
    long countUnread(Long userId);
}
