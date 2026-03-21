package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.Invoice;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.repository.InvoiceRepository;
import DAJ2EE.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ReportController {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/admin/reports")
    public String adminReports(Model model, Authentication auth) {
        if(auth != null) {
            model.addAttribute("fullName", auth.getName());
        } else {
            model.addAttribute("fullName", "Admin");
        }
        
        List<Object[]> revenueMonthly = invoiceRepository.getMonthlyRevenue();
        model.addAttribute("revenueMonthly", revenueMonthly);
        
        // Form to add mock invoices
        List<User> tenants = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "ROLE_TENANT".equals(u.getRole().getName()))
                .toList();
        model.addAttribute("tenants", tenants);
        return "admin/reports";
    }

    @PostMapping("/admin/reports/add-invoice")
    public String addInvoice(@RequestParam Long tenantId,
                             @RequestParam int month,
                             @RequestParam int year,
                             @RequestParam double rentAmount,
                             @RequestParam double electricUsage,
                             @RequestParam double electricCost,
                             @RequestParam double waterUsage,
                             @RequestParam double waterCost) {
        Invoice invoice = new Invoice();
        User tenant = userRepository.findById(tenantId).orElse(null);
        invoice.setTenant(tenant);
        invoice.setMonth(month);
        invoice.setYear(year);
        invoice.setRentAmount(rentAmount);
        invoice.setElectricUsage(electricUsage);
        invoice.setElectricCost(electricCost);
        invoice.setWaterUsage(waterUsage);
        invoice.setWaterCost(waterCost);
        invoice.setTotalAmount(rentAmount + electricCost + waterCost);
        invoice.setStatus("PAID");
        
        invoiceRepository.save(invoice);
        
        return "redirect:/admin/reports?success";
    }

    @GetMapping("/tenant/reports")
    public String tenantReports(Model model, Authentication auth) {
        if (auth == null) return "redirect:/login";
        
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        model.addAttribute("fullName", user != null ? user.getFullName() : auth.getName());
        
        if (user != null) {
            List<Invoice> invoices = invoiceRepository.findByTenantIdOrderByYearDescMonthDesc(user.getId());
            model.addAttribute("invoices", invoices);
        }
        
        return "tenant/reports";
    }
}
