package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    List<Contract> findByTenantId(Long tenantId);
    List<Contract> findByRoomId(Long roomId);
}
