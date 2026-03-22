package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.Invoice;
import DAJ2EE.demo.entity.Payment;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.repository.InvoiceRepository;
import DAJ2EE.demo.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DebtService {
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public List<Invoice> getInvoicesByUser(User user) {
        return invoiceRepository.findByUser(user);
    }

    public List<Payment> getPaymentHistory(User user) {
        return paymentRepository.findByUser(user);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public void processPayment(Long invoiceId, Double amount, String method, String note) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        Payment payment = Payment.builder()
                .invoice(invoice)
                .amount(amount)
                .paymentDate(LocalDateTime.now())
                .method(method)
                .note(note)
                .build();

        paymentRepository.save(payment);

        invoice.setPaidAmount(invoice.getPaidAmount() + amount);
        if (invoice.getPaidAmount() >= invoice.getTotalAmount()) {
            invoice.setStatus("PAID");
        } else {
            invoice.setStatus("PARTIAL");
        }
        invoiceRepository.save(invoice);
    }

    public Double calculateTotalDebt(User user) {
        return invoiceRepository.findByUser(user).stream()
                .filter(i -> !"PAID".equals(i.getStatus()))
                .mapToDouble(Invoice::getRemainingAmount)
                .sum();
    }
}
