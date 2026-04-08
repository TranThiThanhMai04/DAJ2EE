package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.Contract;
import DAJ2EE.demo.entity.ContractStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    @EntityGraph(attributePaths = {"room", "tenant"})
    List<Contract> findByRoomIdAndStatus(Long roomId, ContractStatus status);

    @EntityGraph(attributePaths = {"room", "tenant"})
    List<Contract> findByRoomIdAndStatusIn(Long roomId, List<ContractStatus> statuses);

    @EntityGraph(attributePaths = {"room", "tenant"})
    List<Contract> findAllByStatus(ContractStatus status);

    @EntityGraph(attributePaths = {"room", "tenant"})
    List<Contract> findByTenantIdAndStatus(Long tenantId, ContractStatus status);

    @EntityGraph(attributePaths = {"room", "tenant"})
    List<Contract> findByTenantId(Long tenantId);

    @EntityGraph(attributePaths = {"room", "tenant"})
    List<Contract> findByTenantIdAndStatusIn(Long tenantId, List<ContractStatus> statuses);

    long countByRoomIdAndStatusInAndIdNot(Long roomId, List<ContractStatus> statuses, Long id);

    @Query("SELECT COUNT(c) FROM Contract c WHERE c.status = :status")
    long countActiveContracts(ContractStatus status);
}
