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

        // Nhóm theo tenant (Ưu tiên lấy từ Contract, nếu không thì lấy từ User liên kết trực tiếp)
        Map<Long, List<Invoice>> groupedByTenant = new HashMap<>();
        for (Invoice inv : unpaidInvoices) {
            Long tid = null;
            if (inv.getContract() != null && inv.getContract().getTenant() != null) {
                tid = inv.getContract().getTenant().getId();
            } else if (inv.getUser() != null) {
                tid = inv.getUser().getId();
            }
            
            if (tid != null) {
                groupedByTenant.computeIfAbsent(tid, k -> new ArrayList<>()).add(inv);
            }
        }

        List<DebtSummaryDto> result = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        for (Map.Entry<Long, List<Invoice>> entry : groupedByTenant.entrySet()) {
            List<Invoice> tenantInvoices = entry.getValue();
            Invoice firstInvoice = tenantInvoices.get(0);
            
            User tenant = (firstInvoice.getContract() != null && firstInvoice.getContract().getTenant() != null)
                    ? firstInvoice.getContract().getTenant() : firstInvoice.getUser();

            BigDecimal totalDebt = tenantInvoices.stream()
                    .map(Invoice::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalPaid = tenantInvoices.stream()
                    .map(inv -> {
                        BigDecimal sum = paymentHistoryRepository.sumAmountPaidByInvoiceIdAndStatus(inv.getId(), PaymentHistoryStatus.CONFIRMED);
                        return sum != null ? sum : BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Date oldestDue = tenantInvoices.stream()
                    .map(Invoice::getDueDate)
                    .filter(Objects::nonNull)
                    .min(Date::compareTo)
                    .orElse(null);

            // Lấy phòng dự phòng
            String roomNumber = "N/A";
            if (firstInvoice.getContract() != null && firstInvoice.getContract().getRoom() != null) {
                roomNumber = firstInvoice.getContract().getRoom().getRoomNumber();
            } else if (firstInvoice.getRoomName() != null) {
                roomNumber = firstInvoice.getRoomName();
            }

            DebtSummaryDto dto = new DebtSummaryDto();
            dto.setTenantId(tenant.getId());
            dto.setTenantName(tenant.getFullName());
            dto.setTenantPhone(tenant.getUsername());
            dto.setRoomNumber(roomNumber);
            dto.setContractId(firstInvoice.getContract() != null ? firstInvoice.getContract().getId() : null);
            dto.setInvoiceCount(tenantInvoices.size());
            dto.setTotalDebt(totalDebt.subtract(totalPaid));
            dto.setTotalPaid(totalPaid);
            dto.setOldestDueDate(oldestDue != null ? sdf.format(oldestDue) : "N/A");

            result.add(dto);
        }

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

        // Fix NullPointerException: Kiểm tra và lấy tenant an toàn
        User tenant = null;
        if (invoice.getContract() != null && invoice.getContract().getTenant() != null) {
            tenant = invoice.getContract().getTenant();
        } else if (invoice.getUser() != null) {
            tenant = invoice.getUser();
        }

        if (tenant == null) {
            throw new RuntimeException("Hóa đơn này không có thông tin người thuê.");
        }

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

        invoice.setPaymentStatus(PaymentStatus.PAID);
        invoice.setPaymentDate(java.time.LocalDateTime.now());
        invoiceRepository.save(invoice);

        return paymentHistoryRepository.save(ph);
    }
}
