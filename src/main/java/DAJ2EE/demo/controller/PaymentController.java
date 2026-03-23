package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.Invoice;
import DAJ2EE.demo.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final InvoiceService invoiceService;

    @GetMapping("/{invoiceId}")
    public String checkout(@PathVariable("invoiceId") Long invoiceId, Model model) {
        Invoice invoice = invoiceService.getInvoiceById(invoiceId);
        model.addAttribute("invoice", invoice);
        return "payment/checkout";
    }

    @PostMapping("/process")
    public String processPayment(@RequestParam("invoiceId") Long invoiceId, 
                                 @RequestParam("method") String method,
                                 @RequestParam(value = "proofFile", required = false) org.springframework.web.multipart.MultipartFile proofFile) {
        String proofUrl = null;
        if (proofFile != null && !proofFile.isEmpty()) {
            try {
                String fileName = "proof_" + invoiceId + "_" + System.currentTimeMillis() + ".png";
                java.nio.file.Path path = java.nio.file.Paths.get("d:/j2ee/j2ee/src/main/resources/static/uploads/proofs/" + fileName);
                java.nio.file.Files.createDirectories(path.getParent());
                java.nio.file.Files.copy(proofFile.getInputStream(), path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                proofUrl = "/uploads/proofs/" + fileName;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Giả lập xử lý thanh toán thành công
        invoiceService.updatePaymentStatus(invoiceId, "PAID", proofUrl);
        return "redirect:/payment/success?invoiceId=" + invoiceId;
    }

    @GetMapping("/success")
    public String paymentSuccess(@RequestParam("invoiceId") Long invoiceId, Model model) {
        model.addAttribute("invoiceId", invoiceId);
        return "payment/success";
    }
}
