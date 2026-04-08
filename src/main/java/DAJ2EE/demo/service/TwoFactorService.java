package DAJ2EE.demo.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

@Service
public class TwoFactorService {

    private final GoogleAuthenticator googleAuthenticator;

    public TwoFactorService() {
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setWindowSize(3)
                .build();
        this.googleAuthenticator = new GoogleAuthenticator(config);
    }

    public String generateSecretKey() {
        return googleAuthenticator.createCredentials().getKey();
    }

    public boolean verifyCode(String secretKey, int code) {
        if (secretKey == null || secretKey.isBlank()) return false;
        return googleAuthenticator.authorize(secretKey, code);
    }

    public String renderQrCodeBase64DataUrl(String issuer, String accountName, String secretKey) {
        try {
            String otpauth = buildOtpAuthUri(issuer, accountName, secretKey);
            int size = 260;

            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new MultiFormatWriter()
                    .encode(otpauth, BarcodeFormat.QR_CODE, size, size, hints);

            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);

            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo QR Code 2FA", e);
        }
    }

    private String buildOtpAuthUri(String issuer, String accountName, String secretKey) {
        String encIssuer = urlEncode(issuer);
        String encLabel = urlEncode(issuer + ":" + accountName);
        return "otpauth://totp/" + encLabel +
                "?secret=" + urlEncode(secretKey) +
                "&issuer=" + encIssuer +
                "&digits=6";
    }

    private String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

