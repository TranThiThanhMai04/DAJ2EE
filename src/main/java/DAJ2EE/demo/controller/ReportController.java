package DAJ2EE.demo.controller;

import DAJ2EE.demo.repository.RoomRepository;
import DAJ2EE.demo.repository.ContractRepository;
import DAJ2EE.demo.repository.MaintenanceRequestRepository;
import DAJ2EE.demo.repository.InvoiceRepository;
import DAJ2EE.demo.entity.ContractStatus;
import DAJ2EE.demo.entity.Invoice;
import DAJ2EE.demo.entity.RoomStatus;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final InvoiceRepository invoiceRepository;
    private final RoomRepository roomRepository;
    private final ContractRepository contractRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;

    @GetMapping("/revenue")
    public Map<String, Object> getRevenueData(@RequestParam(defaultValue = "2026") int year) {
        List<Object[]> results = invoiceRepository.getMonthlyRevenue(year);
        Double[] revenue = new Double[12];
        for (int i = 0; i < 12; i++) revenue[i] = 0.0;

        for (Object[] row : results) {
            int month = (int) row[0];
            if (month >= 1 && month <= 12) {
                revenue[month - 1] = ((Number) row[1]).doubleValue();
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("activeContracts", contractRepository.countActiveContracts(ContractStatus.ACTIVE));
        summary.put("totalMaintenanceCount", maintenanceRequestRepository.count());
        summary.put("unpaidInvoices", invoiceRepository.countUnpaidInvoices());
        summary.put("totalRevenue", invoiceRepository.getTotalYearlyRevenue(year));
        
        return Map.of(
            "labels", new String[]{"T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12"},
            "data", revenue,
            "summary", summary
        );
    }

    @GetMapping("/room-status")
    public Map<String, Long> getRoomStatus() {
        Map<String, Long> statusCount = new HashMap<>();
        statusCount.put("EMPTY", roomRepository.countByStatus(RoomStatus.EMPTY));
        statusCount.put("OCCUPIED", roomRepository.countByStatus(RoomStatus.OCCUPIED));
        statusCount.put("MAINTENANCE", roomRepository.countByStatus(RoomStatus.MAINTENANCE));
        return statusCount;
    }

    @GetMapping("/export-excel")
    public void exportToExcel(HttpServletResponse response, @RequestParam(defaultValue = "2026") int year) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=bao_cao_doanh_thu_" + year + ".xlsx");

        List<Invoice> invoices = invoiceRepository.findByYear(year);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Báo cáo Doanh thu " + year);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setColor(IndexedColors.WHITE.getIndex());
            font.setBold(true);
            headerStyle.setFont(font);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Mã HĐ", "Phòng", "Khách hàng", "Tháng", "Năm", "Tổng tiền", "Trạng thái", "Ngày lập"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            double totalAll = 0;
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            for (Invoice inv : invoices) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(inv.getId());
                
                // Lấy tên phòng từ Hợp đồng, nếu không có thì lấy từ cột room_name dự phòng
                String roomNumber = "N/A";
                if (inv.getContract() != null && inv.getContract().getRoom() != null) {
                    roomNumber = inv.getContract().getRoom().getRoomNumber();
                } else if (inv.getRoomName() != null) {
                    roomNumber = inv.getRoomName();
                }
                row.createCell(1).setCellValue(roomNumber);

                // Lấy tên khách hàng từ Hợp đồng, nếu không có thì lấy từ User liên kết
                String tenantName = "N/A";
                if (inv.getContract() != null && inv.getContract().getTenant() != null) {
                    tenantName = inv.getContract().getTenant().getFullName();
                } else if (inv.getUser() != null) {
                    tenantName = inv.getUser().getFullName();
                }
                row.createCell(2).setCellValue(tenantName);

                row.createCell(3).setCellValue(inv.getMonth());
                row.createCell(4).setCellValue(inv.getYear());
                row.createCell(5).setCellValue(inv.getTotalAmount().doubleValue());
                row.createCell(6).setCellValue(inv.getPaymentStatus() != null ? inv.getPaymentStatus().name() : "UNPAID");
                
                String dateStr = inv.getIssueDate() != null ? dateFormat.format(inv.getIssueDate()) : "N/A";
                row.createCell(7).setCellValue(dateStr);
                
                totalAll += inv.getTotalAmount().doubleValue();
            }

            Row totalRow = sheet.createRow(rowNum + 1);
            Cell totalLabel = totalRow.createCell(4);
            totalLabel.setCellValue("TỔNG CỘNG:");
            CellStyle boldStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);
            totalLabel.setCellStyle(boldStyle);
            totalRow.createCell(5).setCellValue(totalAll);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(response.getOutputStream());
        }
    }
}
