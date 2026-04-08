package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.Invoice;
import DAJ2EE.demo.entity.PaymentStatus;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.repository.UserRepository;
import DAJ2EE.demo.service.InvoiceService;
import DAJ2EE.demo.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private InvoiceService invoiceService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/tenant")
    public String tenantIndex(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier;
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oAuth2User) {
            identifier = oAuth2User.getAttribute("email");
        } else {
            identifier = auth != null ? auth.getName() : null;
        }

        if (identifier != null) {
            userRepository.findByUsernameOrEmail(identifier, identifier).ifPresent(u -> {
                model.addAttribute("fullName", u.getFullName());
                // Load thông báo thực từ database
                model.addAttribute("notifications", notificationService.getNotificationsForUser(u.getId()));
                model.addAttribute("unreadCount", notificationService.countUnread(u.getId()));

                List<Invoice> tenantInvoices = invoiceService.getInvoicesByTenant(u.getId());
                BigDecimal totalUnpaidAmount = tenantInvoices.stream()
                        .filter(this::isPendingPayment)
                        .map(Invoice::getTotalAmount)
                        .filter(amount -> amount != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                Long payNowInvoiceId = tenantInvoices.stream()
                        .filter(this::isPendingPayment)
                        .sorted(Comparator.comparing(Invoice::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(Invoice::getId)
                        .findFirst()
                        .orElse(null);

                boolean hasUnpaidInvoices = totalUnpaidAmount.compareTo(BigDecimal.ZERO) > 0;
                boolean hasInvoices = !tenantInvoices.isEmpty();

                model.addAttribute("totalUnpaid", formatCurrency(totalUnpaidAmount));
                model.addAttribute("hasUnpaidInvoices", hasUnpaidInvoices);
                model.addAttribute("hasInvoices", hasInvoices);
                model.addAttribute("payNowUrl", payNowInvoiceId != null ? "/payment/" + payNowInvoiceId : "/tenant/invoices");
            });
        }

        // Fallback nếu không load được
        if (!model.containsAttribute("notifications")) {
            model.addAttribute("notifications", Collections.emptyList());
        }

        if (!model.containsAttribute("totalUnpaid")) {
            model.addAttribute("totalUnpaid", formatCurrency(BigDecimal.ZERO));
            model.addAttribute("hasUnpaidInvoices", false);
            model.addAttribute("hasInvoices", false);
            model.addAttribute("payNowUrl", "/tenant/invoices");
        }

        return "tenant/index";
    }

    private boolean isPendingPayment(Invoice invoice) {
        if (invoice == null || invoice.getPaymentStatus() == null) {
            return false;
        }
        return invoice.getPaymentStatus() == PaymentStatus.UNPAID
                || invoice.getPaymentStatus() == PaymentStatus.OVERDUE;
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.setMinimumFractionDigits(0);
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        return numberFormat.format(safeAmount) + " đ";
    }
}
