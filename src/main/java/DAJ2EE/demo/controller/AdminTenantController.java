package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.Tenant;
import DAJ2EE.demo.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/tenants")
public class AdminTenantController {

    @Autowired
    private TenantService tenantService;

    @GetMapping
    public String listTenants(Model model) {
        model.addAttribute("tenants", tenantService.getAllTenants());
        return "admin/tenant/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("tenant", new Tenant());
        return "admin/tenant/form";
    }

    @PostMapping("/save")
    public String saveTenant(@ModelAttribute("tenant") Tenant tenant, RedirectAttributes redirectAttributes) {
        tenantService.saveTenant(tenant);
        redirectAttributes.addFlashAttribute("successMessage", "Người thuê đã được lưu thành công.");
        return "redirect:/admin/tenants";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Tenant tenant = tenantService.getTenantById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid tenant Id:" + id));
        model.addAttribute("tenant", tenant);
        return "admin/tenant/form";
    }

    @GetMapping("/delete/{id}")
    public String deleteTenant(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        tenantService.deleteTenant(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa người thuê.");
        return "redirect:/admin/tenants";
    }
}
