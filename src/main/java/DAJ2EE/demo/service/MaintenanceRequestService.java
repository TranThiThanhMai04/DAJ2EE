package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.MaintenanceRequest;
import DAJ2EE.demo.entity.MaintenanceStatus;
import DAJ2EE.demo.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface MaintenanceRequestService {
    List<MaintenanceRequest> getAllRequests();
    List<MaintenanceRequest> getRequestsByUser(User user);
    MaintenanceRequest getRequestById(Long id);
    MaintenanceRequest createRequest(User user, String description, MultipartFile image) throws IOException;
    MaintenanceRequest updateStatus(Long id, MaintenanceStatus status);
}
