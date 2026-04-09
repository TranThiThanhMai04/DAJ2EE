package DAJ2EE.demo.service;

import DAJ2EE.demo.dto.UsageTrendDto;

public interface UsageAnalyticsService {
    UsageTrendDto getTenantUsageTrend(Long tenantId);
    UsageTrendDto getAdminUsageTrend();
}
