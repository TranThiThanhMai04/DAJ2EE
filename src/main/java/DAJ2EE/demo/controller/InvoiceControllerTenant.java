package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.Invoice;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.service.InvoiceService;
import DAJ2EE.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/tenant/invoices")
@RequiredArgsConstructor
public class InvoiceControllerTenant {
    private final InvoiceService invoiceService;
    private final UserService userService;
    private final DAJ2EE.demo.repository.ServiceUsageRepository serviceUsageRepository;

    @GetMapping
    public String listInvoices(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        List<Invoice> invoices = invoiceService.getInvoicesByTenant(user.getId());
        model.addAttribute("invoices", invoices);
        return "tenant/invoice-list";
    }

    @GetMapping("/{id}")
    public String viewInvoice(@PathVariable("id") Long id, Model model) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        model.addAttribute("invoice", invoice);
        
        // Fetch service usages for this room and month/year
        List<DAJ2EE.demo.entity.ServiceUsage> usages = 
            serviceUsageRepository.findByRoomIdAndMonthAndYear(
                invoice.getContract().getRoom().getId(), 
                invoice.getMonth(), 
                invoice.getYear()
            );
        model.addAttribute("usages", usages);
        
        return "tenant/invoice-detail";
    }
}
