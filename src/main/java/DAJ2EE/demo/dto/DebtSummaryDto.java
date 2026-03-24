package DAJ2EE.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO hiển thị thông tin công nợ của một người thuê
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DebtSummaryDto {
    private Long tenantId;
    private String tenantName;
    private String tenantPhone;  // username (SĐT)
    private String roomNumber;
    private Long contractId;
    private int invoiceCount;        // Tổng hóa đơn chưa thanh toán
    private BigDecimal totalDebt;    // Tổng tiền còn nợ
    private BigDecimal totalPaid;    // Tổng tiền đã thanh toán
    private String oldestDueDate;    // Ngày đáo hạn sớm nhất
}
