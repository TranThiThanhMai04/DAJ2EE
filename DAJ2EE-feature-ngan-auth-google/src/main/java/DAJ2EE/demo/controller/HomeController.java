package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.service.DebtService;
import DAJ2EE.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DebtService debtService;
    private final UserService userService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/admin")
    public String adminIndex(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User admin = userService.getUserByUsername(auth.getName());
        
        model.addAttribute("fullName", admin != null ? admin.getFullName() : "Admin");
        model.addAttribute("invoices", debtService.getAllInvoices());
        model.addAttribute("payments", debtService.getAllPayments());
        
        return "admin/index";
    }

    @GetMapping("/tenant")
    public String tenantIndex(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getUserByUsername(auth.getName());
        
        if (user != null) {
            model.addAttribute("fullName", user.getFullName());
            model.addAttribute("totalUnpaid", String.format("%,.0f đ", debtService.calculateTotalDebt(user)));
            model.addAttribute("invoices", debtService.getInvoicesByUser(user));
            model.addAttribute("payments", debtService.getPaymentHistory(user));
        }
        
        return "tenant/index";
    }
}
