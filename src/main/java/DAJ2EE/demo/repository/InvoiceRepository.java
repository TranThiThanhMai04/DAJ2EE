package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByTenantIdOrderByYearDescMonthDesc(Long tenantId);
    
    @Query("SELECT i.month, i.year, SUM(i.totalAmount) as totalRevenue " +
           "FROM Invoice i " +
           "WHERE i.status = 'PAID' " +
           "GROUP BY i.year, i.month " +
           "ORDER BY i.year DESC, i.month DESC")
    List<Object[]> getMonthlyRevenue();
}
