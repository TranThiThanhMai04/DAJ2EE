package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.MaintenanceRequest;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.service.CurrentUserService;
import DAJ2EE.demo.service.MaintenanceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/tenant/maintenance")
@RequiredArgsConstructor
public class TenantMaintenanceController {

    private final MaintenanceRequestService maintenanceRequestService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String listRequests(Model model, Authentication authentication) {
        User user = currentUserService.getRequiredUser(authentication);
        List<MaintenanceRequest> requests = maintenanceRequestService.getRequestsByUser(user);
        model.addAttribute("requests", requests);
        return "tenant/maintenance-requests";
    }

    @PostMapping("/create")
    public String createRequest(@RequestParam("description") String description,
                                @RequestParam(value = "image", required = false) MultipartFile image,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = currentUserService.getRequiredUser(authentication);
            maintenanceRequestService.createRequest(user, description, image);
            redirectAttributes.addFlashAttribute("successDetails", "Đã gửi yêu cầu sửa chữa thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorDetails", e.getMessage());
        }
        return "redirect:/tenant/maintenance";
    }
}
