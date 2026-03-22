package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.Invoice;
import DAJ2EE.demo.entity.Payment;
import DAJ2EE.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Query("SELECT p FROM Payment p WHERE p.invoice.user = :user")
    List<Payment> findByUser(User user);
    
    List<Payment> findByInvoice(Invoice invoice);
}
