package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.ServiceUsage;
import java.util.List;

public interface ServiceUsageService {
    List<ServiceUsage> getAllUsage();
    List<ServiceUsage> getUsageByMonthAndYear(Integer month, Integer year);
    ServiceUsage saveUsage(Long roomId, String serviceName, Integer reading, Integer month, Integer year);
    List<ServiceUsage> getUsageByRoom(Long roomId);
}
