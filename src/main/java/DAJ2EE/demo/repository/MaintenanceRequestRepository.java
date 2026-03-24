package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.MaintenanceRequest;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.entity.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {
    List<MaintenanceRequest> findByUserOrderByIdDesc(User user);
    List<MaintenanceRequest> findAllByOrderByIdDesc();

    @Query("SELECT COUNT(m) FROM MaintenanceRequest m WHERE m.status = :status")
    long countByStatus(MaintenanceStatus status);
}
