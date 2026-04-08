package DAJ2EE.demo.controller;

import DAJ2EE.demo.dto.DebtSummaryDto;
import DAJ2EE.demo.entity.PaymentHistory;
import DAJ2EE.demo.service.DebtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/debts")
@RequiredArgsConstructor
public class DebtController {

    private final DebtService debtService;

    /**
     * Trang quản lý công nợ - Hiển thị ai chưa đóng tiền, số tiền còn thiếu
     */
    @GetMapping
    public String debtDashboard(Model model) {
        List<DebtSummaryDto> debtSummaries = debtService.getAllDebtSummaries();
        model.addAttribute("debtSummaries", debtSummaries);

        // Thống kê tổng
        java.math.BigDecimal totalDebtAll = debtSummaries.stream()
                .map(DebtSummaryDto::getTotalDebt)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        model.addAttribute("totalDebtAll", totalDebtAll);
        model.addAttribute("totalDebtors", debtSummaries.size());

        return "admin/debt-dashboard";
    }

    /**
     * Trang lịch sử thanh toán (tất cả)
     */
    @GetMapping("/history")
    public String allPaymentHistory(Model model) {
        List<PaymentHistory> histories = debtService.getAllPaymentHistory();
        model.addAttribute("histories", histories);
        return "admin/payment-history";
    }

    /**
     * Lịch sử thanh toán theo tenant
     */
    @GetMapping("/history/tenant/{tenantId}")
    public String paymentHistoryByTenant(@PathVariable("tenantId") Long tenantId, Model model) {
        List<PaymentHistory> histories = debtService.getPaymentHistoryByTenant(tenantId);
        model.addAttribute("histories", histories);
        model.addAttribute("tenantId", tenantId);
        if (!histories.isEmpty()) {
            model.addAttribute("tenantName", histories.get(0).getTenant().getFullName());
        }
        return "admin/payment-history";
    }

    /**
     * Xác nhận thanh toán hóa đơn → tạo bản ghi payment history
     */
    @PostMapping("/pay")
    public String confirmPayment(@RequestParam("invoiceId") Long invoiceId,
                                 @RequestParam("tenantId") Long tenantId,
                                 @RequestParam(value = "method", defaultValue = "CASH") String method,
                                 @RequestParam(value = "note", required = false) String note,
                                 Authentication authentication) {
        debtService.recordPayment(invoiceId, tenantId, method, note, authentication.getName());
        return "redirect:/admin/debts";
    }
}
