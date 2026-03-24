package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.MaintenanceRequest;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.service.MaintenanceRequestService;
import DAJ2EE.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final UserService userService;

    @GetMapping
    public String listRequests(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        List<MaintenanceRequest> requests = maintenanceRequestService.getRequestsByUser(user);
        model.addAttribute("requests", requests);
        return "tenant/maintenance-requests";
    }

    @PostMapping("/create")
    public String createRequest(@RequestParam("description") String description,
                                @RequestParam(value = "image", required = false) MultipartFile image,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
            maintenanceRequestService.createRequest(user, description, image);
            redirectAttributes.addFlashAttribute("successDetails", "Đã gửi yêu cầu sửa chữa thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorDetails", e.getMessage());
        }
        return "redirect:/tenant/maintenance";
    }
}
