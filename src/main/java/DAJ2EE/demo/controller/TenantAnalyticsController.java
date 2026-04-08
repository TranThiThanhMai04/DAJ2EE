package DAJ2EE.demo.controller;

import DAJ2EE.demo.dto.UsageTrendDto;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.service.CurrentUserService;
import DAJ2EE.demo.service.UsageAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/analytics")
@RequiredArgsConstructor
public class TenantAnalyticsController {

    private final CurrentUserService currentUserService;
    private final UsageAnalyticsService usageAnalyticsService;

    @GetMapping("/usage-trend")
    public ResponseEntity<UsageTrendDto> getTenantUsageTrend(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            User user = currentUserService.getRequiredUser(authentication);
            return ResponseEntity.ok(usageAnalyticsService.getTenantUsageTrend(user.getId()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(401).build();
        }
    }
}
