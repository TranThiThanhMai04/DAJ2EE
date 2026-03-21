package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    List<Notification> findAllByOrderByCreatedAtDesc();
}
