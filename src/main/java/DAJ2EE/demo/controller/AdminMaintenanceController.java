package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.MaintenanceRequest;
import DAJ2EE.demo.entity.MaintenanceStatus;
import DAJ2EE.demo.service.MaintenanceRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/maintenance")
public class AdminMaintenanceController {

    @Autowired
    private MaintenanceRequestService maintenanceRequestService;

    @GetMapping
    public String listRequests(Model model) {
        List<MaintenanceRequest> requests = maintenanceRequestService.getAllRequests();
        model.addAttribute("requests", requests);
        return "admin/maintenance-requests";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, 
                               @RequestParam("status") MaintenanceStatus status,
                               RedirectAttributes redirectAttributes) {
        
        MaintenanceRequest updated = maintenanceRequestService.updateStatus(id, status);
        if (updated != null) {
            redirectAttributes.addFlashAttribute("successDetails", "Cập nhật trạng thái thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorDetails", "Không tìm thấy yêu cầu.");
        }
        return "redirect:/admin/maintenance";
    }
}
