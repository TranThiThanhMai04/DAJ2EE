package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.RepairRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepairRequestRepository extends JpaRepository<RepairRequest, Long> {
    List<RepairRequest> findByTenantIdOrderByIdDesc(Long tenantId);
    List<RepairRequest> findAllByOrderByIdDesc();
    
    // Tìm yêu cầu mới nhất của người thuê dựa trên createdAt hoặc Id
    RepairRequest findFirstByTenantIdOrderByIdDesc(Long tenantId);
}
