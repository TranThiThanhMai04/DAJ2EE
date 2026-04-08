package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.Invoice;
import DAJ2EE.demo.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceNotificationDispatcherImpl implements InvoiceNotificationDispatcher {

    private final NotificationService notificationService;
    private final JavaMailSender mailSender;

    @Value("${app.portal.base-url:http://localhost:8080}")
    private String portalBaseUrl;

    @Value("${app.notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.notification.email.from:}")
    private String fromEmail;

    @Value("${app.notification.zalo.enabled:false}")
    private boolean zaloEnabled;

    @Value("${app.notification.zalo.webhook-url:}")
    private String zaloWebhookUrl;

    @Override
    public void notifyInvoiceCreated(Invoice invoice) {
        if (invoice == null) {
            return;
        }

        User tenant = resolveTenant(invoice);
        if (tenant == null) {
            log.warn("Cannot dispatch invoice notification because tenant is missing for invoice id={}", invoice.getId());
            return;
        }

        String invoiceLink = buildAbsoluteUrl("/tenant/invoices/" + invoice.getId());
        String paymentLink = buildAbsoluteUrl("/payment/" + invoice.getId());
        String roomName = resolveRoomName(invoice);
        String amountText = formatCurrency(invoice.getTotalAmount());
        String periodText = String.format("%02d/%d", invoice.getMonth(), invoice.getYear());

        String inAppTitle = "Hóa đơn mới " + periodText;
        String inAppContent = "Phòng " + roomName + " có hóa đơn mới " + amountText
                + ". Vui lòng thanh toán tại: " + paymentLink;

        sendInAppNotification(tenant, inAppTitle, inAppContent);
        sendEmailNotification(tenant, roomName, periodText, amountText, invoiceLink, paymentLink);
        sendZaloNotification(tenant, invoice.getId(), roomName, periodText, amountText, paymentLink);
    }

    private void sendInAppNotification(User tenant, String title, String content) {
        try {
            notificationService.sendNotificationToUser(tenant.getId(), title, content);
        } catch (Exception ex) {
            log.error("Failed to create in-app invoice notification for userId={}: {}", tenant.getId(), ex.getMessage());
        }
    }

    private void sendEmailNotification(User tenant,
                                       String roomName,
                                       String periodText,
                                       String amountText,
                                       String invoiceLink,
                                       String paymentLink) {
        if (!emailEnabled) {
            log.info("Skipping invoice email because app.notification.email.enabled=false (userId={})", tenant.getId());
            return;
        }

        String recipientEmail = resolveRecipientEmail(tenant);
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.info("Skipping invoice email because recipient email is empty. userId={}", tenant.getId());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            String configuredFrom = resolveFromEmail();
            if (configuredFrom != null && !configuredFrom.isBlank()) {
                message.setFrom(configuredFrom);
            }
            message.setTo(recipientEmail);
            message.setSubject("[SmartCity] Hóa đơn mới kỳ " + periodText + " - Phòng " + roomName);
            message.setText(buildEmailBody(tenant.getFullName(), roomName, periodText, amountText, invoiceLink, paymentLink));
            mailSender.send(message);
            log.info("Invoice email sent successfully to {} for userId={} invoiceId={}", recipientEmail, tenant.getId(), extractInvoiceId(invoiceLink));
        } catch (Exception ex) {
            log.error("Failed to send invoice email for userId={}: {}", tenant.getId(), ex.getMessage(), ex);
        }
    }

    private String resolveRecipientEmail(User tenant) {
        if (tenant == null) {
            return null;
        }

        if (tenant.getEmail() != null && !tenant.getEmail().isBlank()) {
            return tenant.getEmail().trim();
        }

        String username = tenant.getUsername();
        if (username != null && username.contains("@")) {
            return username.trim();
        }

        return null;
    }

    private String extractInvoiceId(String invoiceLink) {
        if (invoiceLink == null || invoiceLink.isBlank()) {
            return "N/A";
        }
        int idx = invoiceLink.lastIndexOf('/');
        if (idx < 0 || idx + 1 >= invoiceLink.length()) {
            return "N/A";
        }
        return invoiceLink.substring(idx + 1);
    }

    private void sendZaloNotification(User tenant,
                                      Long invoiceId,
                                      String roomName,
                                      String periodText,
                                      String amountText,
                                      String paymentLink) {
        if (!zaloEnabled || zaloWebhookUrl == null || zaloWebhookUrl.isBlank()) {
            return;
        }

        String recipient = tenant.getUsername();
        if (recipient == null || recipient.isBlank()) {
            log.info("Skipping Zalo invoice notification because username/phone is empty. userId={}", tenant.getId());
            return;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("channel", "zalo");
            payload.put("to", recipient);
            payload.put("contentType", MediaType.APPLICATION_JSON_VALUE);
            payload.put("title", "Hoa don moi " + periodText);
            payload.put("message", "Phong " + roomName + " co hoa don " + amountText + ". Thanh toan nhanh: " + paymentLink);
            payload.put("invoiceId", invoiceId);
            payload.put("paymentLink", paymentLink);

            restTemplate.postForEntity(zaloWebhookUrl, payload, Void.class);
        } catch (RestClientResponseException ex) {
            log.error("Zalo webhook responded with error status {} for userId={}: {}",
                    ex.getStatusCode().value(), tenant.getId(), ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            log.error("Failed to send Zalo invoice notification for userId={}: {}", tenant.getId(), ex.getMessage());
        }
    }

    private User resolveTenant(Invoice invoice) {
        if (invoice.getContract() != null && invoice.getContract().getTenant() != null) {
            return invoice.getContract().getTenant();
        }
        return invoice.getUser();
    }

    private String resolveRoomName(Invoice invoice) {
        if (invoice.getContract() != null && invoice.getContract().getRoom() != null
                && invoice.getContract().getRoom().getRoomNumber() != null) {
            return invoice.getContract().getRoom().getRoomNumber();
        }
        return invoice.getRoomName() != null ? invoice.getRoomName() : "N/A";
    }

    private String buildAbsoluteUrl(String path) {
        String baseUrl = portalBaseUrl != null ? portalBaseUrl.trim() : "http://localhost:8080";
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        return baseUrl + path;
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.setMinimumFractionDigits(0);
        BigDecimal safe = amount != null ? amount : BigDecimal.ZERO;
        return numberFormat.format(safe) + " đ";
    }

    private String buildEmailBody(String fullName,
                                  String roomName,
                                  String periodText,
                                  String amountText,
                                  String invoiceLink,
                                  String paymentLink) {
        String residentName = fullName != null && !fullName.isBlank() ? fullName : "Quý cư dân";

        return "Xin chào " + residentName + ",\n\n"
                + "Hệ thống vừa tạo hóa đơn mới cho phòng " + roomName + " (kỳ " + periodText + ").\n"
                + "Tổng tiền cần thanh toán: " + amountText + "\n\n"
                + "Xem chi tiết hóa đơn: " + invoiceLink + "\n"
                + "Thanh toán nhanh: " + paymentLink + "\n\n"
                + "Trân trọng,\n"
                + "SmartCity Tenant Portal";
    }

    private String resolveFromEmail() {
        if (fromEmail != null && !fromEmail.isBlank()) {
            return fromEmail;
        }
        if (mailSender instanceof JavaMailSenderImpl javaMailSender) {
            return javaMailSender.getUsername();
        }
        return null;
    }
}
