package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.Invoice;
import DAJ2EE.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByUser(User user);
    List<Invoice> findByStatus(String status);
}
