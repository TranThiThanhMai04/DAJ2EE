package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.Contract;
import java.util.List;
import java.util.Optional;

public interface ContractService {
    List<Contract> getAllContracts();
    Optional<Contract> getContractById(Long id);
    Contract saveContract(Contract contract);
    void deleteContract(Long id);
    List<Contract> getContractsByTenantId(Long tenantId);
    List<Contract> getContractsByRoomId(Long roomId);
}
