package DAJ2EE.demo.controller;

import DAJ2EE.demo.dto.TenantRequestDto;
import DAJ2EE.demo.entity.Contract;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.service.ContractService;
import DAJ2EE.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/tenant")
@PreAuthorize("hasRole('TENANT')")
public class TenantUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ContractService contractService;

    // Lấy User đang đăng nhập
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("Không tìm thấy người dùng hiện tại");
        }
        return user;
    }

    @GetMapping("/contracts")
    public String myContracts(Model model) {
        User currentUser = getCurrentUser();
        List<Contract> contracts = contractService.getContractsByTenant(currentUser.getId());
        model.addAttribute("contracts", contracts);
        return "tenant/contracts";
    }

    @PostMapping("/contracts/{id}/confirm")
    public String confirmContract(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            contractService.confirmContract(id, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận hợp đồng thành công! Chúc bạn ở vui vẻ.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/tenant/contracts";
    }

    @GetMapping("/contracts/{id}")
    public String viewContractDetail(@PathVariable("id") Long id, Model model) {
        User currentUser = getCurrentUser();
        DAJ2EE.demo.entity.Contract contract = contractService.getContractById(id);
        if (!contract.getTenant().getId().equals(currentUser.getId())) {
            return "redirect:/tenant/contracts";
        }
        model.addAttribute("contract", contract);
        return "tenant/contract-detail";
    }

    @GetMapping("/profile")
    public String viewProfile(Model model) {
        User currentUser = getCurrentUser();
        TenantRequestDto dto = new TenantRequestDto();
        dto.setFullName(currentUser.getFullName());
        dto.setPhone(currentUser.getUsername());
        dto.setEmail(currentUser.getEmail());
        dto.setCccd(currentUser.getCccd());
        model.addAttribute("profileDto", dto);
        model.addAttribute("user", currentUser); // Added for modern profile template
        return "tenant/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute("profileDto") TenantRequestDto dto,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (result.hasFieldErrors("phone") || result.hasFieldErrors("password") || result.hasFieldErrors("confirmPassword")) {
            return "tenant/profile";
        }
        try {
            User currentUser = getCurrentUser();
            
            dto.setFullName(currentUser.getFullName());
            dto.setEmail(currentUser.getEmail());
            dto.setCccd(currentUser.getCccd());

            userService.updateTenant(currentUser.getId(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thành công. Nếu đổi số điện thoại, bạn cần đăng nhập lại!");
            return "redirect:/login"; 
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/tenant/profile";
        }
    }
}
