package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.Contract;
import DAJ2EE.demo.entity.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    List<Contract> findByRoomIdAndStatus(Long roomId, ContractStatus status);
    List<Contract> findByRoomIdAndStatusIn(Long roomId, List<ContractStatus> statuses);
    List<Contract> findAllByStatus(ContractStatus status);
    List<Contract> findByTenantIdAndStatus(Long tenantId, ContractStatus status);
    List<Contract> findByTenantId(Long tenantId);
    List<Contract> findByTenantIdAndStatusIn(Long tenantId, List<ContractStatus> statuses);

    @Query("SELECT COUNT(c) FROM Contract c WHERE c.status = :status")
    long countActiveContracts(ContractStatus status);
}
