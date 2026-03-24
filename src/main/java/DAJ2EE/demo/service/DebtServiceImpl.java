package DAJ2EE.demo.service;

import DAJ2EE.demo.dto.DebtSummaryDto;
import DAJ2EE.demo.entity.*;
import DAJ2EE.demo.repository.InvoiceRepository;
import DAJ2EE.demo.repository.PaymentHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DebtServiceImpl implements DebtService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;

    @Override
    public List<DebtSummaryDto> getAllDebtSummaries() {
        // Lấy tất cả hóa đơn chưa thanh toán (UNPAID hoặc OVERDUE)
        List<Invoice> allInvoices = invoiceRepository.findAll();
        List<Invoice> unpaidInvoices = allInvoices.stream()
                .filter(i -> i.getPaymentStatus() == PaymentStatus.UNPAID
                        || i.getPaymentStatus() == PaymentStatus.OVERDUE)
                .collect(Collectors.toList());

        // Nhóm theo tenant
        Map<Long, List<Invoice>> groupedByTenant = unpaidInvoices.stream()
                .filter(i -> i.getContract() != null && i.getContract().getTenant() != null)
                .collect(Collectors.groupingBy(i -> i.getContract().getTenant().getId()));

        List<DebtSummaryDto> result = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        for (Map.Entry<Long, List<Invoice>> entry : groupedByTenant.entrySet()) {
            List<Invoice> tenantInvoices = entry.getValue();
            Invoice firstInvoice = tenantInvoices.get(0);
            User tenant = firstInvoice.getContract().getTenant();

            BigDecimal totalDebt = tenantInvoices.stream()
                    .map(Invoice::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Tính tổng đã trả (từ payment_history CONFIRMED)
            BigDecimal totalPaid = tenantInvoices.stream()
                    .map(inv -> {
                        BigDecimal sum = paymentHistoryRepository.sumAmountPaidByInvoiceIdAndStatus(inv.getId(), PaymentHistoryStatus.CONFIRMED);
                        return sum != null ? sum : BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Tìm ngày đáo hạn sớm nhất
            Date oldestDue = tenantInvoices.stream()
                    .map(Invoice::getDueDate)
                    .filter(Objects::nonNull)
                    .min(Date::compareTo)
                    .orElse(null);

            // Lấy phòng từ hợp đồng
            String roomNumber = firstInvoice.getContract().getRoom() != null
                    ? firstInvoice.getContract().getRoom().getRoomNumber()
                    : "N/A";

            DebtSummaryDto dto = new DebtSummaryDto();
            dto.setTenantId(tenant.getId());
            dto.setTenantName(tenant.getFullName());
            dto.setTenantPhone(tenant.getUsername());
            dto.setRoomNumber(roomNumber);
            dto.setContractId(firstInvoice.getContract().getId());
            dto.setInvoiceCount(tenantInvoices.size());
            dto.setTotalDebt(totalDebt.subtract(totalPaid));  // Nợ = Tổng - Đã trả
            dto.setTotalPaid(totalPaid);
            dto.setOldestDueDate(oldestDue != null ? sdf.format(oldestDue) : "N/A");

            result.add(dto);
        }

        // Sắp xếp theo nợ nhiều nhất trước
        result.sort((a, b) -> b.getTotalDebt().compareTo(a.getTotalDebt()));
        return result;
    }

    @Override
    public List<PaymentHistory> getPaymentHistoryByTenant(Long tenantId) {
        return paymentHistoryRepository.findByTenantIdOrderByPaidAtDesc(tenantId);
    }

    @Override
    public List<PaymentHistory> getAllPaymentHistory() {
        return paymentHistoryRepository.findAllByOrderByPaidAtDesc();
    }

    @Override
    public List<PaymentHistory> getPaymentHistoryByInvoice(Long invoiceId) {
        return paymentHistoryRepository.findByInvoiceIdOrderByPaidAtDesc(invoiceId);
    }

    @Override
    @Transactional
    public PaymentHistory recordPayment(Long invoiceId, Long tenantId, String method, String note, String confirmedBy) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn ID: " + invoiceId));

        User tenant = invoice.getContract().getTenant();
        if (!tenant.getId().equals(tenantId)) {
            throw new RuntimeException("Hóa đơn không thuộc về tenant này.");
        }

        PaymentHistory ph = new PaymentHistory();
        ph.setInvoice(invoice);
        ph.setTenant(tenant);
        ph.setAmountPaid(invoice.getTotalAmount());
        ph.setPaymentMethod(method != null ? method : "CASH");
        ph.setTransactionNote(note);
        ph.setPaidAt(java.time.LocalDateTime.now());
        ph.setConfirmedBy(confirmedBy);
        ph.setConfirmedAt(java.time.LocalDateTime.now());
        ph.setStatus(PaymentHistoryStatus.CONFIRMED);

        // Cập nhật trạng thái hóa đơn sang PAID
        invoice.setPaymentStatus(PaymentStatus.PAID);
        invoice.setPaymentDate(java.time.LocalDateTime.now());
        invoiceRepository.save(invoice);

        return paymentHistoryRepository.save(ph);
    }
}
