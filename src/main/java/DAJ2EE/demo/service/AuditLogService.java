package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.AuditLog;
import java.util.List;

public interface AuditLogService {
    void log(String action, String details);
    List<AuditLog> getAllLogs();
}
