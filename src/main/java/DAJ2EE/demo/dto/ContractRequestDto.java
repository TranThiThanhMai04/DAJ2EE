package DAJ2EE.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ContractRequestDto {
    @NotNull(message = "Phòng không được để trống")
    private Long roomId;

    @NotNull(message = "Người thuê không được để trống")
    private Long tenantId;

    @NotNull(message = "Tên theo CCCD không được để trống")
    private String tenantFullName;

    @NotNull(message = "Quê quán không được để trống")
    private String tenantHometown;

    @NotNull(message = "Giới tính không được để trống")
    private String tenantGender;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @DecimalMin(value = "0.0", message = "Tiền cọc phải lớn hơn hoặc bằng 0")
    private BigDecimal deposit;
    
    @NotNull(message = "Giá thuê phòng không được để trống")
    @DecimalMin(value = "0.0", message = "Giá thuê phòng không hợp lệ")
    private BigDecimal monthlyRent;
}
