package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.Contract;
import DAJ2EE.demo.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContractServiceImpl implements ContractService {

    @Autowired
    private ContractRepository contractRepository;

    @Override
    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
    }

    @Override
    public Optional<Contract> getContractById(Long id) {
        return contractRepository.findById(id);
    }

    @Override
    public Contract saveContract(Contract contract) {
        return contractRepository.save(contract);
    }

    @Override
    public void deleteContract(Long id) {
        contractRepository.deleteById(id);
    }

    @Override
    public List<Contract> getContractsByTenantId(Long tenantId) {
        return contractRepository.findByTenantId(tenantId);
    }

    @Override
    public List<Contract> getContractsByRoomId(Long roomId) {
        return contractRepository.findByRoomId(roomId);
    }
}
