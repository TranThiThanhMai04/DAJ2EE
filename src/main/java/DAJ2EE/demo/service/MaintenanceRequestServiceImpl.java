package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.*;
import DAJ2EE.demo.repository.ContractRepository;
import DAJ2EE.demo.repository.MaintenanceRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MaintenanceRequestServiceImpl implements MaintenanceRequestService {

    @Autowired
    private MaintenanceRequestRepository maintenanceRequestRepository;

    @Autowired
    private ContractRepository contractRepository;

    private final String UPLOAD_DIR = "uploads/";

    @Override
    public List<MaintenanceRequest> getAllRequests() {
        return maintenanceRequestRepository.findAllByOrderByIdDesc();
    }

    @Override
    public List<MaintenanceRequest> getRequestsByUser(User user) {
        return maintenanceRequestRepository.findByUserOrderByIdDesc(user);
    }

    @Override
    public MaintenanceRequest getRequestById(Long id) {
        return maintenanceRequestRepository.findById(id).orElse(null);
    }

    @Override
    public MaintenanceRequest createRequest(User user, String description, MultipartFile image) throws IOException {
        // Tìm hợp đồng active của người dùng
        List<Contract> activeContracts = contractRepository.findByTenantIdAndStatus(user.getId(), ContractStatus.ACTIVE);
        if (activeContracts == null || activeContracts.isEmpty()) {
            throw new RuntimeException("Bạn không có phòng đang thuê nào, không thể tạo yêu cầu sửa chữa.");
        }
        
        // Lấy phòng từ hợp đồng active
        Room currentRoom = activeContracts.get(0).getRoom();

        MaintenanceRequest request = new MaintenanceRequest();
        request.setUser(user);
        request.setRoom(currentRoom);
        request.setDescription(description);
        request.setStatus(MaintenanceStatus.PENDING);

        // Xử lý upload ảnh nếu có
        if (image != null && !image.isEmpty()) {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            String originalFileName = StringUtils.cleanPath(image.getOriginalFilename());
            String extension = "";
            int extIndex = originalFileName.lastIndexOf(".");
            if (extIndex > 0) {
                extension = originalFileName.substring(extIndex);
            }
            String fileName = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Set dường dẫn (relative phục vụ web mapper)
            request.setImageUrl("/uploads/" + fileName);
        }

        return maintenanceRequestRepository.save(request);
    }

    @Override
    public MaintenanceRequest updateStatus(Long id, MaintenanceStatus status) {
        Optional<MaintenanceRequest> requestOpt = maintenanceRequestRepository.findById(id);
        if (requestOpt.isPresent()) {
            MaintenanceRequest req = requestOpt.get();
            req.setStatus(status);
            return maintenanceRequestRepository.save(req);
        }
        return null;
    }
}
