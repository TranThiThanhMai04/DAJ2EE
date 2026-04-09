package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.Contract;
import DAJ2EE.demo.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractNotificationDispatcherImpl implements ContractNotificationDispatcher {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NotificationService notificationService;
    private final JavaMailSender mailSender;

    @Value("${app.portal.base-url:http://localhost:8080}")
    private String portalBaseUrl;

    @Value("${app.notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.notification.email.from:}")
    private String fromEmail;

    @Override
    public void notifyContractCreated(Contract contract) {
        if (contract == null || contract.getId() == null) {
            return;
        }

        User tenant = contract.getTenant();
        if (tenant == null) {
            log.warn("Cannot dispatch contract notification because tenant is missing for contract id={}", contract.getId());
            return;
        }

        String roomName = resolveRoomName(contract);
        String contractLink = buildAbsoluteUrl("/tenant/contracts/" + contract.getId());
        String periodText = formatPeriod(contract.getStartDate(), contract.getEndDate());
        String rentText = formatCurrency(contract.getMonthlyRent());

        String title = "Hợp đồng mới chờ xác nhận";
        String content = "Bạn có hợp đồng mới cho phòng " + roomName
                + " (" + periodText + "). Vui lòng xem chi tiết và xác nhận tại: " + contractLink;

        sendInAppNotification(tenant, title, content);
        sendEmailNotificationForGoogleTenant(tenant, roomName, periodText, rentText, contractLink);
    }

    private void sendInAppNotification(User tenant, String title, String content) {
        try {
            notificationService.sendNotificationToUser(tenant.getId(), title, content);
        } catch (Exception ex) {
            log.error("Failed to create in-app contract notification for userId={}: {}", tenant.getId(), ex.getMessage());
        }
    }

    private void sendEmailNotificationForGoogleTenant(User tenant,
                                                      String roomName,
                                                      String periodText,
                                                      String rentText,
                                                      String contractLink) {
        if (!emailEnabled) {
            return;
        }

        if (!isGoogleTenant(tenant)) {
            return;
        }

        String recipientEmail = resolveRecipientEmail(tenant);
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.info("Skipping contract email because recipient email is empty. userId={}", tenant.getId());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            String configuredFrom = resolveFromEmail();
            if (configuredFrom != null && !configuredFrom.isBlank()) {
                message.setFrom(configuredFrom);
            }

            message.setTo(recipientEmail);
            message.setSubject("[SmartCity] Hợp đồng mới cần xác nhận - Phòng " + roomName);
            message.setText(buildEmailBody(tenant.getFullName(), roomName, periodText, rentText, contractLink));
            mailSender.send(message);
            log.info("Contract email sent successfully to {} for userId={} contractLink={}", recipientEmail, tenant.getId(), contractLink);
        } catch (Exception ex) {
            log.error("Failed to send contract email for userId={}: {}", tenant.getId(), ex.getMessage(), ex);
        }
    }

    private boolean isGoogleTenant(User tenant) {
        if (tenant == null) {
            return false;
        }

        if (tenant.getProvider() != null && "GOOGLE".equalsIgnoreCase(tenant.getProvider().trim())) {
            return true;
        }

        // Legacy fallback: nhiều tài khoản Google cũ có username=email nhưng provider chưa set chuẩn
        String username = tenant.getUsername();
        String email = tenant.getEmail();
        return username != null
                && email != null
                && !username.isBlank()
                && !email.isBlank()
                && username.equalsIgnoreCase(email);
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

    private String resolveRoomName(Contract contract) {
        if (contract.getRoom() != null && contract.getRoom().getRoomNumber() != null) {
            return contract.getRoom().getRoomNumber();
        }
        return "N/A";
    }

    private String formatPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return "không xác định";
        }
        return startDate.format(DATE_FORMATTER) + " - " + endDate.format(DATE_FORMATTER);
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
                                  String rentText,
                                  String contractLink) {
        String residentName = fullName != null && !fullName.isBlank() ? fullName : "Quý cư dân";

        return "Xin chào " + residentName + ",\n\n"
                + "Admin vừa tạo hợp đồng mới cho phòng " + roomName + ".\n"
                + "Thời hạn hợp đồng: " + periodText + "\n"
                + "Giá thuê hàng tháng: " + rentText + "\n\n"
                + "Vui lòng xem chi tiết và xác nhận hợp đồng tại: " + contractLink + "\n\n"
                + "Trân trọng,\n"
                + "SmartCity Tenant Portal";
    }

    private String buildAbsoluteUrl(String path) {
        String baseUrl = portalBaseUrl != null ? portalBaseUrl.trim() : "http://localhost:8080";
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        return baseUrl + path;
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