package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.Invoice;
import DAJ2EE.demo.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;
    private final DAJ2EE.demo.repository.RoomRepository roomRepository;
    private final DAJ2EE.demo.repository.ServiceUsageRepository serviceUsageRepository;

    @GetMapping
    public String listInvoices(Model model) {
        List<Invoice> invoices = invoiceService.getAllInvoices();
        model.addAttribute("invoices", invoices);
        model.addAttribute("rooms", roomRepository.findAllByOrderByRoomNumberAsc());
        return "admin/invoice-list";
    }

    @PostMapping("/generate")
    public String generateInvoice(@RequestParam("roomId") Long roomId, @RequestParam("month") int month, @RequestParam("year") int year) {
        invoiceService.createMonthlyInvoice(roomId, month, year);
        return "redirect:/admin/invoices";
    }

    @PostMapping("/{id}/pay")
    public String markAsPaid(@PathVariable("id") Long id) {
        invoiceService.updatePaymentStatus(id, "PAID", null);
        return "redirect:/admin/invoices";
    }

    @PostMapping("/{id}/reject")
    public String rejectPayment(@PathVariable("id") Long id, @RequestParam("reason") String reason) {
        invoiceService.rejectPayment(id, reason);
        return "redirect:/admin/invoices/" + id;
    }

    @GetMapping("/{id}")
    public String viewInvoiceDetail(@PathVariable("id") Long id, Model model) {
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
        
        return "admin/invoice-detail";
    }
}
