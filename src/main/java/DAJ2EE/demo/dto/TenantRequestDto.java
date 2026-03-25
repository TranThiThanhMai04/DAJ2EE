package DAJ2EE.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TenantRequestDto {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0(3|5|7|8|9)\\d{8}$", message = "Số điện thoại không hợp lệ")
    private String phone; // username

    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "CCCD không được để trống")
    @Pattern(regexp = "^\\d{12}$", message = "CCCD phải đúng 12 số")
    private String cccd;

    // Password có thể trống nếu không muốn đổi khi update
    private String password;
    
    private String confirmPassword;
}
