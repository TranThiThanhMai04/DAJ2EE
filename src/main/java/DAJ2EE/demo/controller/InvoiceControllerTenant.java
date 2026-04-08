package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.Invoice;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.service.CurrentUserService;
import DAJ2EE.demo.service.InvoiceService;
import DAJ2EE.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/tenant/invoices")
@RequiredArgsConstructor
public class InvoiceControllerTenant {
    private final InvoiceService invoiceService;
    private final CurrentUserService currentUserService;
    private final DAJ2EE.demo.repository.ServiceUsageRepository serviceUsageRepository;

    @GetMapping
    public String listInvoices(Model model, Authentication authentication) {
        User user = currentUserService.getRequiredUser(authentication);
        List<Invoice> invoices = invoiceService.getInvoicesByTenant(user.getId());
        model.addAttribute("invoices", invoices);
        model.addAttribute("user", user);
        model.addAttribute("fullName", user.getFullName());
        return "tenant/invoice-list";
    }

    @GetMapping("/{id}")
    public String viewInvoice(@PathVariable("id") Long id, Model model, Authentication authentication) {
        User user = currentUserService.getRequiredUser(authentication);
        Invoice invoice = invoiceService.getInvoiceById(id);

        if (invoice.getContract() == null
                || invoice.getContract().getTenant() == null
                || !invoice.getContract().getTenant().getId().equals(user.getId())) {
            return "redirect:/tenant/invoices";
        }

        model.addAttribute("invoice", invoice);
        model.addAttribute("user", user);
        model.addAttribute("fullName", user.getFullName());

        List<DAJ2EE.demo.entity.ServiceUsage> usages = Collections.emptyList();
        if (invoice.getContract().getRoom() != null) {
            usages = serviceUsageRepository.findByRoomIdAndMonthAndYear(
                    invoice.getContract().getRoom().getId(),
                    invoice.getMonth(),
                    invoice.getYear()
            );
        }
        model.addAttribute("usages", usages);

        return "tenant/invoice-detail";
    }
}
