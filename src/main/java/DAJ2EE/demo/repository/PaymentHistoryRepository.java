package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.PaymentHistory;
import DAJ2EE.demo.entity.PaymentHistoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {

    // Lịch sử thanh toán theo tenant
    List<PaymentHistory> findByTenantIdOrderByPaidAtDesc(Long tenantId);

    // Lịch sử thanh toán theo hóa đơn
    List<PaymentHistory> findByInvoiceIdOrderByPaidAtDesc(Long invoiceId);

    // Tổng số tiền đã thanh toán cho 1 hóa đơn (chỉ tính CONFIRMED)
    @Query("SELECT SUM(ph.amountPaid) FROM PaymentHistory ph WHERE ph.invoice.id = :invoiceId AND ph.status = :status")
    BigDecimal sumAmountPaidByInvoiceIdAndStatus(@Param("invoiceId") Long invoiceId, @Param("status") PaymentHistoryStatus status);

    // Tất cả lịch sử thanh toán (admin xem)
    List<PaymentHistory> findAllByOrderByPaidAtDesc();

    // Lịch sử theo phòng (thông qua invoice -> contract -> room)
    @Query("SELECT ph FROM PaymentHistory ph WHERE ph.invoice.contract.room.id = :roomId ORDER BY ph.paidAt DESC")
    List<PaymentHistory> findByRoomId(@Param("roomId") Long roomId);
}
