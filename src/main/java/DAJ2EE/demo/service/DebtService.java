package DAJ2EE.demo.service;

import DAJ2EE.demo.dto.DebtSummaryDto;
import DAJ2EE.demo.entity.PaymentHistory;

import java.util.List;

public interface DebtService {

    /**
     * Lấy danh sách công nợ tất cả tenant (ai chưa đóng, còn thiếu bao nhiêu)
     */
    List<DebtSummaryDto> getAllDebtSummaries();

    /**
     * Lấy danh sách lịch sử thanh toán của 1 tenant
     */
    List<PaymentHistory> getPaymentHistoryByTenant(Long tenantId);

    /**
     * Lấy tất cả lịch sử thanh toán (admin)
     */
    List<PaymentHistory> getAllPaymentHistory();

    /**
     * Lấy lịch sử thanh toán theo hóa đơn
     */
    List<PaymentHistory> getPaymentHistoryByInvoice(Long invoiceId);

    /**
     * Ghi nhận thanh toán (tạo payment history khi admin xác nhận PAID)
     */
    PaymentHistory recordPayment(Long invoiceId, Long tenantId, String method, String note, String confirmedBy);
}
