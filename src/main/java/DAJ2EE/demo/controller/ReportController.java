package DAJ2EE.demo.controller;

import DAJ2EE.demo.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class ReportController {
    private final InvoiceService invoiceService;

    @GetMapping("/reports")
    public String revenueReport(Model model, 
                               @RequestParam(required = false) Integer month, 
                               @RequestParam(required = false) Integer year) {
        if (month == null) month = LocalDate.now().getMonthValue();
        if (year == null) year = LocalDate.now().getYear();

        BigDecimal revenue = invoiceService.getRevenue(month, year);
        model.addAttribute("revenue", revenue);
        model.addAttribute("month", month);
        model.addAttribute("year", year);
        
        return "admin/reports";
    }
}
