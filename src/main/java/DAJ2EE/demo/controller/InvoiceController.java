package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.Invoice;
import DAJ2EE.demo.entity.Contract;
import DAJ2EE.demo.entity.ContractStatus;
import DAJ2EE.demo.service.InvoiceService;
import DAJ2EE.demo.service.ServiceUsageService;
import DAJ2EE.demo.dto.LatestReadingsResponse;
import DAJ2EE.demo.repository.RoomRepository;
import DAJ2EE.demo.repository.ServiceUsageRepository;
import DAJ2EE.demo.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;
    private final ServiceUsageService serviceUsageService;
    private final RoomRepository roomRepository;
    private final ServiceUsageRepository serviceUsageRepository;
    private final ContractRepository contractRepository;

    @GetMapping
    public String listInvoices(Model model) {
        List<Invoice> invoices = invoiceService.getAllInvoices();
        model.addAttribute("invoices", invoices);
        model.addAttribute("rooms", roomRepository.findAllByOrderByRoomNumberAsc());
        model.addAttribute("services", serviceUsageService.getAllServices()); // Thêm đơn giá dịch vụ
        return "admin/invoice-list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("rooms", roomRepository.findAllByOrderByRoomNumberAsc());
        model.addAttribute("services", serviceUsageService.getAllServices());
        return "admin/invoice-create";
    }

    @PostMapping("/generate")
    public String generateInvoice(
            @RequestParam("roomId") Long roomId, 
            @RequestParam("month") int month, 
            @RequestParam("year") int year,
            @RequestParam(value = "elecReading", required = false) Integer elecReading,
            @RequestParam(value = "waterReading", required = false) Integer waterReading,
            @RequestParam(value = "otherFees", required = false) java.math.BigDecimal otherFees,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            invoiceService.createMonthlyInvoice(roomId, month, year, elecReading, waterReading, otherFees);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu chỉ số và tạo hóa đơn thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
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

    @GetMapping("/latest-readings")
    @ResponseBody
    public ResponseEntity<LatestReadingsResponse> getLatestReadings(
            @RequestParam("roomId") Long roomId, 
            @RequestParam("month") int month, 
            @RequestParam("year") int year) {
        
        
        Integer lastElec = serviceUsageService.getLatestReadingBefore(roomId, "Điện", month, year);
        Integer lastWater = serviceUsageService.getLatestReadingBefore(roomId, "Nước", month, year);
        
        LatestReadingsResponse res = new LatestReadingsResponse();
        res.setLastElecReading(lastElec);
        res.setLastWaterReading(lastWater);
        
        // Tìm hợp đồng ACTIVE hoặc PENDING cho phòng này
        List<ContractStatus> validStatuses = Arrays.asList(ContractStatus.ACTIVE, ContractStatus.PENDING);
        List<Contract> contracts = contractRepository.findByRoomIdAndStatusIn(roomId, validStatuses);
        contracts.stream().findFirst().ifPresent(c -> {
            res.setMonthlyRent(c.getMonthlyRent());
            res.setArea(c.getRoom().getAreaM2() != null ? c.getRoom().getAreaM2().doubleValue() : 0.0);
        });
            
        return ResponseEntity.ok(res);
    }
}
