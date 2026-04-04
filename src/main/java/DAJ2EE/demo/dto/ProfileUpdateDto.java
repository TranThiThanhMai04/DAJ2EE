package DAJ2EE.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO cho chức năng cập nhật hồ sơ cá nhân.
 * Chỉ chứa trường fullName để ngăn chặn Mass Assignment.
 * Các trường như SĐT (username), email là không thể thay đổi từ phía người dùng.
 */
@Getter
@Setter
public class ProfileUpdateDto {

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

}
