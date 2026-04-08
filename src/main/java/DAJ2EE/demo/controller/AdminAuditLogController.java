package DAJ2EE.demo.controller;

import DAJ2EE.demo.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public String showAuditLogs(Model model) {
        model.addAttribute("logs", auditLogService.getAllLogs());
        model.addAttribute("fullName", "Admin Panel");
        return "admin/audit-logs";
    }
}
