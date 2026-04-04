package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByContractRoomId(Long roomId);
    List<Invoice> findByContractTenantId(Long tenantId);
    Optional<Invoice> findByContractIdAndMonthAndYear(Long contractId, int month, int year);
    
    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.contract c LEFT JOIN FETCH c.room r LEFT JOIN FETCH c.tenant t LEFT JOIN FETCH i.user u WHERE i.year = :year")
    List<Invoice> findByYear(@org.springframework.data.repository.query.Param("year") int year);
    
    @org.springframework.data.jpa.repository.Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.paymentStatus = 'PAID' AND i.month = :month AND i.year = :year")
    java.math.BigDecimal sumPaidAmountByMonthAndYear(@org.springframework.data.repository.query.Param("month") int month, @org.springframework.data.repository.query.Param("year") int year);
    @Query("SELECT i.month, SUM(i.totalAmount) FROM Invoice i WHERE i.year = :year GROUP BY i.month ORDER BY i.month ASC")
    List<Object[]> getMonthlyRevenue(int year);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.paymentStatus = 'UNPAID'")
    long countUnpaidInvoices();

    @Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.year = :year")
    Double getTotalYearlyRevenue(int year);
}
