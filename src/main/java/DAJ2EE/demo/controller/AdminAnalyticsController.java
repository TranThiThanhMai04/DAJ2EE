package DAJ2EE.demo.controller;

import DAJ2EE.demo.dto.UsageTrendDto;
import DAJ2EE.demo.service.UsageAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final UsageAnalyticsService usageAnalyticsService;

    @GetMapping("/usage-trend")
    public ResponseEntity<UsageTrendDto> getAdminUsageTrend() {
        return ResponseEntity.ok(usageAnalyticsService.getAdminUsageTrend());
    }
}
