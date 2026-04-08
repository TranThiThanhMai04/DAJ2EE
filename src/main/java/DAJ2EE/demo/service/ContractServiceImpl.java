package DAJ2EE.demo.service;

import DAJ2EE.demo.dto.ContractRequestDto;
import DAJ2EE.demo.entity.Contract;
import DAJ2EE.demo.entity.ContractStatus;
import DAJ2EE.demo.entity.Room;
import DAJ2EE.demo.entity.RoomStatus;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.exception.ResourceNotFoundException;
import DAJ2EE.demo.repository.ContractRepository;
import DAJ2EE.demo.repository.RoomRepository;
import DAJ2EE.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContractServiceImpl implements ContractService {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = false)
    public List<Contract> getAllContracts() {
        List<Contract> contracts = contractRepository.findAll();
        lazyUpdateContractStatuses(contracts);
        return contracts;
    }

    private void lazyUpdateContractStatuses(List<Contract> contracts) {
        for (Contract contract : contracts) {
            if (contract.getStatus() == ContractStatus.ACTIVE && contract.getEndDate().isBefore(java.time.LocalDate.now())) {
                contract.setStatus(ContractStatus.EXPIRED);
                Room room = contract.getRoom();
                room.setStatus(RoomStatus.EMPTY);
                roomRepository.save(room);
                contractRepository.save(contract);
            }
        }
    }

    @Override
    public Contract getContractById(Long id) {
        return contractRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng"));
    }

    @Override
    @Transactional
    public Contract createContract(ContractRequestDto dto) {
        if (dto.getStartDate().isAfter(dto.getEndDate()) || dto.getStartDate().isEqual(dto.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        Room room = roomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng"));

        if (room.getStatus() != RoomStatus.EMPTY) {
            throw new IllegalArgumentException("Phòng này hiện không trống để cho thuê");
        }

        List<ContractStatus> activeOrPending = java.util.Arrays.asList(ContractStatus.ACTIVE, ContractStatus.PENDING);
        List<Contract> existingContracts = contractRepository.findByRoomIdAndStatusIn(room.getId(), activeOrPending);
        if (!existingContracts.isEmpty()) {
            throw new IllegalArgumentException("Phòng này đang có hợp đồng có hiệu lực hoặc đang chờ xác nhận");
        }

        User tenant = userRepository.findById(dto.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách thuê"));

        tenant.setFullName(dto.getTenantFullName());
        tenant.setHometown(dto.getTenantHometown());
        tenant.setGender(dto.getTenantGender());
        userRepository.save(tenant);

        List<ContractStatus> activeOrPendingStatuses = java.util.Arrays.asList(ContractStatus.ACTIVE, ContractStatus.PENDING);
        List<Contract> tenantContracts = contractRepository.findByTenantIdAndStatusIn(tenant.getId(), activeOrPendingStatuses);
        if (tenantContracts.size() >= 5) {
            throw new IllegalArgumentException("Mỗi người thuê chỉ được thuê tối đa 5 phòng (bao gồm cả hợp đồng đang hiệu lực và đang chờ xác nhận).");
        }

        Contract contract = new Contract();
        contract.setRoom(room);
        contract.setTenant(tenant);
        contract.setStartDate(dto.getStartDate());
        contract.setEndDate(dto.getEndDate());
        contract.setDeposit(dto.getDeposit());
        contract.setMonthlyRent(dto.getMonthlyRent());
        contract.setStatus(ContractStatus.PENDING);

        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);
        
        return contractRepository.save(contract);
    }

    @Override
    @Transactional
    public Contract terminateContract(Long id) {
        Contract contract = getContractById(id);
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new IllegalArgumentException("Chỉ có thể kết thúc hợp đồng đang có hiệu lực");
        }

        contract.setStatus(ContractStatus.EXPIRED);
        
        Room room = contract.getRoom();
        room.setStatus(RoomStatus.EMPTY);
        roomRepository.save(room);

        return contractRepository.save(contract);
    }

    @Override
    @Transactional
    public void confirmContract(Long id, Long tenantId) {
        Contract contract = getContractById(id);
        
        if (!contract.getTenant().getId().equals(tenantId)) {
            throw new IllegalArgumentException("Bạn không có quyền xác nhận hợp đồng này");
        }
        
        if (contract.getStatus() != ContractStatus.PENDING) {
            throw new IllegalArgumentException("Khách chỉ có thể xác nhận hợp đồng ở trạng thái Chờ xác nhận (PENDING)");
        }

        List<Contract> activeContracts = contractRepository.findByTenantIdAndStatus(tenantId, ContractStatus.ACTIVE);
        if (activeContracts.size() >= 5) {
            throw new IllegalArgumentException("Bạn đã đạt giới hạn tối đa 5 hợp đồng Đang hiệu lực (ACTIVE).");
        }

        contract.setStatus(ContractStatus.ACTIVE);

        Room room = contract.getRoom();
        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);

        contractRepository.save(contract);
    }

    @Override
    @Transactional(readOnly = false)
    public List<Contract> getContractsByTenant(Long tenantId) {
        List<Contract> contracts = contractRepository.findByTenantId(tenantId);
        lazyUpdateContractStatuses(contracts);
        return contracts;
    }
}
