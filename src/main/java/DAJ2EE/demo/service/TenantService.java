package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.Tenant;
import java.util.List;
import java.util.Optional;

public interface TenantService {
    List<Tenant> getAllTenants();
    Optional<Tenant> getTenantById(Long id);
    Tenant saveTenant(Tenant tenant);
    void deleteTenant(Long id);
    boolean existsByCccd(String cccd);
}
