package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.PaymentHistory;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.service.DebtService;
import DAJ2EE.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/tenant/payment-history")
@RequiredArgsConstructor
public class TenantPaymentHistoryController {

    private final DebtService debtService;
    private final UserService userService;

    /**
     * Người thuê xem lịch sử thanh toán của chính mình
     */
    @GetMapping
    public String myPaymentHistory(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        List<PaymentHistory> histories = debtService.getPaymentHistoryByTenant(user.getId());
        model.addAttribute("histories", histories);
        model.addAttribute("tenantName", user.getFullName());
        return "tenant/payment-history";
    }
}
