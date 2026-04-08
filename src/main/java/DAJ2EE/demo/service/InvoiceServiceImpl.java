package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.*;
import DAJ2EE.demo.repository.ContractRepository;
import DAJ2EE.demo.repository.InvoiceRepository;
import DAJ2EE.demo.repository.ServiceUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final ContractRepository contractRepository;
    private final ServiceUsageRepository serviceUsageRepository;
    private final ServiceUsageService serviceUsageService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public Invoice createMonthlyInvoice(Long roomId, int month, int year, Integer elecReading, Integer waterReading, BigDecimal otherFees) {
        // Tìm hợp đồng đang hoạt động hoặc chờ xác nhận cho phòng này
        List<ContractStatus> validStatuses = Arrays.asList(ContractStatus.ACTIVE, ContractStatus.PENDING);
        List<Contract> activeContracts = contractRepository.findByRoomIdAndStatusIn(roomId, validStatuses);
        if (activeContracts.isEmpty()) {
            throw new RuntimeException("Phòng không có hợp đồng hoạt động (ACTIVE/PENDING) để tính hóa đơn.");
        }
        Contract contract = activeContracts.get(0);

        // Kiểm tra xem đã có hóa đơn cho tháng/năm này chưa
        invoiceRepository.findByContractIdAndMonthAndYear(contract.getId(), month, year)
                .ifPresent(i -> { throw new RuntimeException("Hóa đơn cho tháng này đã tồn tại."); });

        // Nếu admin cung cấp chỉ số, lưu chúng trước.
        // Sau đó hợp nhất với dữ liệu vừa truy vấn để không bị phụ thuộc vào việc native insert
        // có được đọc lại ngay trong cùng luồng hay không.
        Map<String, ServiceUsage> usagesByService = new LinkedHashMap<>();
        mergeUsages(usagesByService, serviceUsageRepository.findByRoomIdAndMonthAndYear(roomId, month, year));

        if (elecReading != null) {
            usagesByService.put(normalizeServiceKey("Điện"),
                    serviceUsageService.saveUsage(roomId, "Điện", elecReading, month, year));
        }
        if (waterReading != null) {
            usagesByService.put(normalizeServiceKey("Nước"),
                    serviceUsageService.saveUsage(roomId, "Nước", waterReading, month, year));
        }

        // Lấy danh sách chỉ số dịch vụ (điện, nước...) cho phòng trong tháng/năm này
        mergeUsages(usagesByService, serviceUsageRepository.findByRoomIdAndMonthAndYear(roomId, month, year));
        List<ServiceUsage> usages = new ArrayList<>(usagesByService.values());
        
        // Kiểm tra xem đã nhập đủ cả Điện và Nước chưa
        boolean hasElec = usagesByService.containsKey(normalizeServiceKey("Điện"));
        boolean hasWater = usagesByService.containsKey(normalizeServiceKey("Nước"));
        
        if (!hasElec || !hasWater) {
            String missing = (!hasElec && !hasWater) ? "Điện và Nước" : (!hasElec ? "Điện" : "Nước");
            throw new RuntimeException("Vui lòng nhập chỉ số " + missing + " cho tháng " + month + "/" + year + " trước khi tạo hóa đơn.");
        }

        // Tính tổng tiền dịch vụ (Điện, Nước)
        BigDecimal totalServiceAmount = usages.stream()
                .map(ServiceUsage::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // --- CHI TIẾT CÁC KHOẢN PHÍ ---
        BigDecimal totalOtherFees = otherFees;
        if (totalOtherFees == null) {
            // Tính toán mặc định nếu không cung cấp phí cụ thể
            BigDecimal area = BigDecimal.valueOf(contract.getRoom().getAreaM2() == null ? 0.0 : contract.getRoom().getAreaM2().doubleValue());
            BigDecimal managementFee = area.multiply(BigDecimal.valueOf(5000));
            BigDecimal basicServiceFee = BigDecimal.valueOf(100000);
            BigDecimal parkingFee = BigDecimal.valueOf(150000);
            BigDecimal internetFee = BigDecimal.valueOf(250000);
            totalOtherFees = managementFee.add(basicServiceFee).add(parkingFee).add(internetFee);
        }

        // Tổng tiền hóa đơn = Thuê phòng + Điện/Nước + Các phí khác (Quản lý, Dịch vụ...)
        BigDecimal totalAmount = contract.getMonthlyRent()
                .add(totalServiceAmount)
                .add(totalOtherFees);

        // Tạo hóa đơn mới
        Invoice invoice = new Invoice();
        invoice.setContract(contract);
        invoice.setUser(contract.getTenant());
        invoice.setRoomName(contract.getRoom().getRoomNumber());
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

        Invoice savedInvoice = invoiceRepository.save(invoice);
        
        // Ghi nhật ký hệ thống
        auditLogService.log("Tạo hóa đơn", "Đã tạo hóa đơn mới cho phòng " + contract.getRoom().getRoomNumber() + 
            " (Tháng " + month + "/" + year + ") - Tổng tiền: " + totalAmount.toString() + "đ");
            
        return savedInvoice;
    }

    private void mergeUsages(Map<String, ServiceUsage> usagesByService, List<ServiceUsage> usages) {
        for (ServiceUsage usage : usages) {
            String key = normalizeServiceKey(usage);
            if (key != null) {
                usagesByService.put(key, usage);
            }
        }
    }

    private String normalizeServiceKey(ServiceUsage usage) {
        if (usage == null || usage.getService() == null) {
            return null;
        }
        return normalizeServiceKey(usage.getService().getName());
    }

    private String normalizeServiceKey(String serviceName) {
        if (serviceName == null) {
            return null;
        }

        String normalized = Normalizer.normalize(serviceName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .trim()
                .toLowerCase(Locale.ROOT);

        return normalized.isEmpty() ? null : normalized;
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
