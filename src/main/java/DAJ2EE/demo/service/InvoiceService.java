package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.Invoice;

import java.util.List;

public interface InvoiceService {
    Invoice createMonthlyInvoice(Long roomId, int month, int year);
    List<Invoice> getAllInvoices();
    List<Invoice> getInvoicesByRoom(Long roomId);
    List<Invoice> getInvoicesByTenant(Long tenantId);
    Invoice getInvoiceById(Long id);
    void updatePaymentStatus(Long id, String status, String proofUrl);
    void rejectPayment(Long id, String reason);
    java.math.BigDecimal getRevenue(int month, int year);
}
