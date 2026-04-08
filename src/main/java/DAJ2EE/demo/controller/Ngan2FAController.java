package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.entity.UserOTP;
import DAJ2EE.demo.repository.UserOTPRepository;
import DAJ2EE.demo.repository.UserRepository;
import DAJ2EE.demo.service.TwoFactorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.SecureRandom;
import java.util.Optional;

import static DAJ2EE.demo.security.TwoFactorAuthenticationSuccessHandler.SESSION_IS_2FA_REQUIRED;
import static DAJ2EE.demo.security.TwoFactorAuthenticationSuccessHandler.SESSION_IS_2FA_VERIFIED;
import static DAJ2EE.demo.security.TwoFactorAuthenticationSuccessHandler.SESSION_POST_LOGIN_REDIRECT;

@Controller
public class Ngan2FAController {

    private final UserRepository userRepository;
    private final UserOTPRepository userOTPRepository;
    private final TwoFactorService twoFactorService;

    public Ngan2FAController(UserRepository userRepository, UserOTPRepository userOTPRepository, TwoFactorService twoFactorService) {
        this.userRepository = userRepository;
        this.userOTPRepository = userOTPRepository;
        this.twoFactorService = twoFactorService;
    }

    @GetMapping("/2fa/setup")
    public String setup2fa(Model model) {
        User user = requireCurrentUser();

        UserOTP otp = userOTPRepository.findById(user.getId()).orElse(null);
        if (otp == null) {
            otp = new UserOTP();
            otp.setUser(user);
            otp.setSecretKey(twoFactorService.generateSecretKey());
            otp.setIs2faEnabled(false);
            otp.setBackupCode(generateBackupCode());
            otp = userOTPRepository.save(otp);
        } else if (otp.getSecretKey() == null || otp.getSecretKey().isBlank()) {
            otp.setSecretKey(twoFactorService.generateSecretKey());
            if (otp.getBackupCode() == null || otp.getBackupCode().isBlank()) {
                otp.setBackupCode(generateBackupCode());
            }
            otp = userOTPRepository.save(otp);
        }

        String issuer = "Apartment Management";
        String account = (user.getEmail() != null && !user.getEmail().isBlank()) ? user.getEmail() : user.getUsername();
        String qrCodeUrl = twoFactorService.renderQrCodeBase64DataUrl(issuer, account, otp.getSecretKey());

        boolean isAdmin = user.getRole() != null && "ROLE_ADMIN".equals(user.getRole().getName());
        model.addAttribute("qrCodeUrl", qrCodeUrl);
        model.addAttribute("backupCode", otp.getBackupCode());
        model.addAttribute("isEnabled", Boolean.TRUE.equals(otp.getIs2faEnabled()));

        return isAdmin ? "admin/setup-2fa" : "tenant/setup-2fa";
    }

    @PostMapping("/2fa/enable")
    public String enable2fa(@RequestParam("verificationCode") String verificationCode,
                            RedirectAttributes redirectAttributes,
                            HttpSession session) {
        User user = requireCurrentUser();
        UserOTP otp = userOTPRepository.findById(user.getId()).orElse(null);
        if (otp == null || otp.getSecretKey() == null || otp.getSecretKey().isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần tạo mã QR trước khi kích hoạt 2FA.");
            return "redirect:/2fa/setup";
        }

        int code;
        try {
            code = Integer.parseInt(verificationCode.trim());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã OTP không hợp lệ (phải là 6 chữ số).");
            return "redirect:/2fa/setup";
        }

        if (!twoFactorService.verifyCode(otp.getSecretKey(), code)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã OTP không đúng. Vui lòng thử lại.");
            return "redirect:/2fa/setup";
        }

        otp.setIs2faEnabled(true);
        userOTPRepository.save(otp);

        // Nếu đang ở flow bắt buộc 2FA sau login: cho phép verify ngay hoặc đánh dấu verified luôn.
        if (Boolean.TRUE.equals(session.getAttribute(SESSION_IS_2FA_REQUIRED))) {
            session.setAttribute(SESSION_IS_2FA_VERIFIED, true);
        }

        redirectAttributes.addFlashAttribute("successMessage", "Kích hoạt 2FA thành công.");
        return "redirect:/2fa/setup";
    }

    @GetMapping("/auth/verify-2fa")
    public String showVerify2fa(Model model, HttpSession session) {
        Boolean required = (Boolean) session.getAttribute(SESSION_IS_2FA_REQUIRED);
        Boolean verified = (Boolean) session.getAttribute(SESSION_IS_2FA_VERIFIED);

        if (Boolean.TRUE.equals(verified) || required == null || !required) {
            String redirect = (String) session.getAttribute(SESSION_POST_LOGIN_REDIRECT);
            return "redirect:" + (redirect != null ? redirect : "/");
        }

        model.addAttribute("postLoginRedirect", session.getAttribute(SESSION_POST_LOGIN_REDIRECT));
        return "auth/verify-2fa";
    }

    @PostMapping("/auth/verify-2fa")
    public String verify2fa(@RequestParam("otpCode") String otpCode,
                            RedirectAttributes redirectAttributes,
                            HttpSession session) {
        User user = requireCurrentUser();
        UserOTP otp = userOTPRepository.findById(user.getId()).orElse(null);

        if (otp == null || otp.getSecretKey() == null || otp.getSecretKey().isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản chưa cài đặt 2FA. Vui lòng thiết lập trước.");
            return "redirect:/2fa/setup";
        }

        String cleaned = otpCode == null ? "" : otpCode.replaceAll("\\s+", "").toUpperCase();

        // Cho phép dùng backup code (tùy chọn)
        if (otp.getBackupCode() != null && !otp.getBackupCode().isBlank() && cleaned.equals(otp.getBackupCode())) {
            otp.setBackupCode(generateBackupCode()); // rotate để tránh reuse
            userOTPRepository.save(otp);
            session.setAttribute(SESSION_IS_2FA_VERIFIED, true);
            redirectAttributes.addFlashAttribute("successMessage", "Xác thực thành công bằng mã dự phòng.");
            String redirect = (String) session.getAttribute(SESSION_POST_LOGIN_REDIRECT);
            return "redirect:" + (redirect != null ? redirect : "/");
        }

        int code;
        try {
            code = Integer.parseInt(cleaned);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã OTP không hợp lệ (6 số) hoặc sai mã dự phòng.");
            return "redirect:/auth/verify-2fa";
        }

        boolean ok = twoFactorService.verifyCode(otp.getSecretKey(), code);
        if (!ok) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã OTP không đúng. Vui lòng thử lại.");
            return "redirect:/auth/verify-2fa";
        }

        session.setAttribute(SESSION_IS_2FA_VERIFIED, true);
        String redirect = (String) session.getAttribute(SESSION_POST_LOGIN_REDIRECT);
        return "redirect:" + (redirect != null ? redirect : "/");
    }

    private User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier;
        if (auth != null && auth.getPrincipal() instanceof OAuth2User oAuth2User) {
            Object email = oAuth2User.getAttributes().get("email");
            identifier = email != null ? String.valueOf(email) : auth.getName();
        } else {
            identifier = auth != null ? auth.getName() : null;
        }

        Optional<User> userOpt = (identifier == null)
                ? Optional.empty()
                : userRepository.findByUsernameOrEmail(identifier, identifier);

        return userOpt.orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng hiện tại"));
    }

    private String generateBackupCode() {
        // 10 ký tự, dễ đọc, tránh gây nhầm lẫn (I,O,0,1)
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}

