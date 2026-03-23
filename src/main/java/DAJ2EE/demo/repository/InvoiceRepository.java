package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByContractRoomId(Long roomId);
    List<Invoice> findByContractTenantId(Long tenantId);
    Optional<Invoice> findByContractIdAndMonthAndYear(Long contractId, int month, int year);
    
    @org.springframework.data.jpa.repository.Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.paymentStatus = 'PAID' AND i.month = :month AND i.year = :year")
    java.math.BigDecimal sumPaidAmountByMonthAndYear(@org.springframework.data.repository.query.Param("month") int month, @org.springframework.data.repository.query.Param("year") int year);
}
