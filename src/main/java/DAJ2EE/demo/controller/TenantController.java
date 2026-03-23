package DAJ2EE.demo.controller;

import DAJ2EE.demo.dto.TenantRequestDto;
import DAJ2EE.demo.dto.UserRegistrationDto;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/tenants")
@PreAuthorize("hasRole('ADMIN')")
public class TenantController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String listTenants(Model model) {
        model.addAttribute("tenants", userService.getAllTenants());
        return "admin/tenants";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("tenantDto", new UserRegistrationDto());
        return "admin/tenant-create";
    }

    @PostMapping("/create")
    public String createTenant(@Valid @ModelAttribute("tenantDto") UserRegistrationDto dto,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/tenant-create";
        }
        try {
            userService.registerTenant(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm khách thuê mới thành công!");
            return "redirect:/admin/tenants";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/tenant-create";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User tenant = userService.getTenantById(id);
            TenantRequestDto dto = new TenantRequestDto();
            dto.setFullName(tenant.getFullName());
            dto.setPhone(tenant.getUsername());
            dto.setEmail(tenant.getEmail());
            dto.setCccd(tenant.getCccd());
            model.addAttribute("tenantDto", dto);
            model.addAttribute("tenantId", id);
            return "admin/tenant-edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/tenants";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateTenant(@PathVariable("id") Long id,
                               @Valid @ModelAttribute("tenantDto") TenantRequestDto dto,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("tenantId", id);
            return "admin/tenant-edit";
        }
        try {
            userService.updateTenant(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công!");
            return "redirect:/admin/tenants";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("tenantId", id);
            return "admin/tenant-edit";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteTenant(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteTenant(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa khách thuê thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/tenants";
    }

    @PostMapping("/approve/{id}")
    public String approveTenant(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.approveTenant(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt tài khoản thành công! Vui lòng tạo hợp đồng.");
            return "redirect:/admin/contracts/create?tenantId=" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/tenants";
        }
    }
}
