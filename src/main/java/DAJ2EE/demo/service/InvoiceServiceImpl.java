package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.*;
import DAJ2EE.demo.repository.ContractRepository;
import DAJ2EE.demo.repository.InvoiceRepository;
import DAJ2EE.demo.repository.ServiceUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final ContractRepository contractRepository;
    private final ServiceUsageRepository serviceUsageRepository;

    @Override
    @Transactional
    public Invoice createMonthlyInvoice(Long roomId, int month, int year) {
        // Tìm hợp đồng đang hoạt động cho phòng này
        List<Contract> activeContracts = contractRepository.findByRoomIdAndStatus(roomId, ContractStatus.ACTIVE);
        if (activeContracts.isEmpty()) {
            throw new RuntimeException("Phòng không có hợp đồng hoạt động (ACTIVE) để tính hóa đơn.");
        }
        Contract contract = activeContracts.get(0);

        // Kiểm tra xem đã có hóa đơn cho tháng/năm này chưa
        invoiceRepository.findByContractIdAndMonthAndYear(contract.getId(), month, year)
                .ifPresent(i -> { throw new RuntimeException("Hóa đơn cho tháng này đã tồn tại."); });

        // Lấy danh sách chỉ số dịch vụ (điện, nước...) cho phòng trong tháng/năm này
        List<ServiceUsage> usages = serviceUsageRepository.findByRoomIdAndMonthAndYear(roomId, month, year);

        // Tính tổng tiền dịch vụ (Điện, Nước)
        BigDecimal totalServiceAmount = usages.stream()
                .map(ServiceUsage::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // --- CHI TIẾT CÁC KHOẢN PHÍ MỚI ---
        // 1. Phí quản lý/vận hành: Diện tích (m2) * 5.000đ
        BigDecimal area = BigDecimal.valueOf(contract.getRoom().getAreaM2() == null ? 0.0 : contract.getRoom().getAreaM2().doubleValue());
        BigDecimal managementFee = area.multiply(BigDecimal.valueOf(5000));

        // 2. Phí dịch vụ cơ bản (Rác, an ninh, vệ sinh...): Cố định 100.000đ
        BigDecimal basicServiceFee = BigDecimal.valueOf(100000);

        // 3. Phí gửi xe (Xe máy/Ô tô): Giả định cố định 150.000đ
        BigDecimal parkingFee = BigDecimal.valueOf(150000);

        // 4. Phí Internet/Truyền hình: Cố định 250.000đ
        BigDecimal internetFee = BigDecimal.valueOf(250000);

        // Tổng tiền hóa đơn = Thuê phòng + Điện/Nước + Quản lý + Dịch vụ + Gửi xe + Internet
        BigDecimal totalAmount = contract.getMonthlyRent()
                .add(totalServiceAmount)
                .add(managementFee)
                .add(basicServiceFee)
                .add(parkingFee)
                .add(internetFee);

        // Tạo hóa đơn mới
        Invoice invoice = new Invoice();
        invoice.setContract(contract);
        invoice.setMonth(month);
        invoice.setYear(year);
        invoice.setTotalAmount(totalAmount);
        invoice.setPaymentStatus(PaymentStatus.UNPAID);
        invoice.setIssueDate(new Date()); // Ngày phát hành là hôm nay
        
        // Hạn thanh toán sau 7 ngày
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, 7);
        invoice.setDueDate(cal.getTime());

        return invoiceRepository.save(invoice);
    }

    @Override
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @Override
    public List<Invoice> getInvoicesByRoom(Long roomId) {
        return invoiceRepository.findByContractRoomId(roomId);
    }

    @Override
    public List<Invoice> getInvoicesByTenant(Long tenantId) {
        return invoiceRepository.findByContractTenantId(tenantId);
    }

    @Override
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn."));
    }

    @Override
    @Transactional
    public void updatePaymentStatus(Long id, String status, String proofUrl) {
        Invoice invoice = getInvoiceById(id);
        invoice.setPaymentStatus(PaymentStatus.valueOf(status));
        if (PaymentStatus.PAID.name().equals(status)) {
            invoice.setPaymentDate(java.time.LocalDateTime.now());
        }
        if (proofUrl != null) {
            invoice.setProofImageUrl(proofUrl);
            invoice.setRejectionReason(null); // Clear reason if new proof uploaded
        }
        invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public void rejectPayment(Long id, String reason) {
        Invoice invoice = getInvoiceById(id);
        invoice.setPaymentStatus(PaymentStatus.UNPAID);
        invoice.setRejectionReason(reason);
        invoice.setProofImageUrl(null); // Clear invalid proof so they can re-upload
        invoiceRepository.save(invoice);
    }

    @Override
    public BigDecimal getRevenue(int month, int year) {
        BigDecimal revenue = invoiceRepository.sumPaidAmountByMonthAndYear(month, year);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
}
