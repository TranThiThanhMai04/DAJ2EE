package DAJ2EE.demo.service;

import DAJ2EE.demo.dto.ContractRequestDto;
import DAJ2EE.demo.entity.Contract;
import java.util.List;

public interface ContractService {
    List<Contract> getAllContracts();
    Contract getContractById(Long id);
    Contract createContract(ContractRequestDto dto);
    Contract terminateContract(Long id);
    void confirmContract(Long id, Long tenantId);
    List<Contract> getContractsByTenant(Long tenantId);
}
