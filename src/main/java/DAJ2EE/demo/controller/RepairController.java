package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.RepairRequest;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.repository.RepairRequestRepository;
import DAJ2EE.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Controller
public class RepairController {

    @Autowired
    private RepairRequestRepository repairRequestRepository;

    @Autowired
    private UserRepository userRepository;

    private static final String UPLOAD_DIR = "uploads/";

    // ================= TENANT ROUTES ==================

    @GetMapping("/tenant/maintenance")
    public String tenantMaintenance(Model model, Principal principal) {
        String username = principal != null ? principal.getName() : null;
        User user = null;
        if (username != null) {
            user = userRepository.findByUsername(username).orElse(null);
        }
        
        // Cố định tạm user id 1 nếu chưa đăng nhập
        if (user == null) {
            user = userRepository.findById(1L).orElse(null);
        }

        if (user != null) {
            List<RepairRequest> requests = repairRequestRepository.findByTenantIdOrderByIdDesc(user.getId());
            model.addAttribute("requests", requests);
        }
        
        model.addAttribute("fullName", user != null ? user.getFullName() : "Người thuê");
        return "tenant/maintenance";
    }

    @GetMapping("/tenant/maintenance/create")
    public String createMaintenanceForm(Model model, Principal principal) {
        String username = principal != null ? principal.getName() : null;
        User user = null;
        if (username != null) user = userRepository.findByUsername(username).orElse(null);
        if (user == null) user = userRepository.findById(1L).orElse(null);

        model.addAttribute("fullName", user != null ? user.getFullName() : "Người thuê");
        return "tenant/maintenance-form";
    }

    @PostMapping("/tenant/maintenance/create")
    public String submitMaintenance(
            @RequestParam("roomNumber") String roomNumber,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("priority") String priority,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String username = principal != null ? principal.getName() : null;
        User user = null;
        if (username != null) user = userRepository.findByUsername(username).orElse(null);
        if (user == null) user = userRepository.findById(1L).orElse(null);

        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin tài khoản! Vui lòng gửi khi đã đăng nhập.");
            return "redirect:/tenant/maintenance";
        }

        // Kiểm tra hợp lệ dữ liệu căn bản
        if (roomNumber == null || roomNumber.trim().isEmpty() || 
            title == null || title.trim().isEmpty() || 
            description == null || description.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng điền đầy đủ các thông tin bắt buộc!");
            return "redirect:/tenant/maintenance/create";
        }

        // Chống gửi trùng lặp (Cooldown 5 phút mỗi user)
        RepairRequest lastRequest = repairRequestRepository.findFirstByTenantIdOrderByIdDesc(user.getId());
        if (lastRequest != null && lastRequest.getCreatedAt() != null) {
            long minutesBetween = ChronoUnit.MINUTES.between(lastRequest.getCreatedAt(), LocalDateTime.now());
            if (minutesBetween < 5) {
                redirectAttributes.addFlashAttribute("error", "Bạn vừa gửi 1 yêu cầu gần đây. Vui lòng đợi 5 phút trước khi tạo mới!");
                return "redirect:/tenant/maintenance";
            }
        }

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            try {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                String filename = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                Path filePath = uploadPath.resolve(filename);
                Files.copy(image.getInputStream(), filePath);
                imageUrl = "/uploads/" + filename;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        RepairRequest request = new RepairRequest();
        request.setTenant(user);
        request.setRoomNumber(roomNumber);
        request.setTitle(title);
        request.setDescription(description);
        request.setPriority(priority); // LOW, MEDIUM, HIGH
        request.setImageUrl(imageUrl);
        request.setStatus("PENDING"); 

        repairRequestRepository.save(request);

        redirectAttributes.addFlashAttribute("success", "Đã gửi yêu cầu sửa chữa thành công!");
        return "redirect:/tenant/maintenance";
    }

    // ================= ADMIN ROUTES ====================

    @GetMapping("/admin/maintenance")
    public String adminMaintenance(Model model) {
        List<RepairRequest> requests = repairRequestRepository.findAllByOrderByIdDesc();
        model.addAttribute("requests", requests);
        return "admin/maintenance";
    }

    @PostMapping("/admin/maintenance/{id}/status")
    public String updateMaintenanceStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            @RequestParam(value = "adminReply", required = false) String adminReply,
            RedirectAttributes redirectAttributes) {
            
        RepairRequest request = repairRequestRepository.findById(id).orElse(null);
        if (request != null) {
            
            String currentStatus = request.getStatus() != null ? request.getStatus() : "PENDING";
            boolean isValid = false;

            // Quy tắc luồng trạng thái
            if ("PENDING".equals(currentStatus) && ("IN_PROGRESS".equals(status) || "REJECTED".equals(status))) {
                isValid = true;
            } else if ("IN_PROGRESS".equals(currentStatus) && "COMPLETED".equals(status)) {
                isValid = true;
            } else if (currentStatus.equals(status)) {
                isValid = true; // Không thay đổi gì
            }

            if (!isValid) {
                redirectAttributes.addFlashAttribute("error", "Chuyển trạng thái không hợp lệ: " + currentStatus + " -> " + status);
                return "redirect:/admin/maintenance";
            }
            
            request.setStatus(status);
            if (adminReply != null && !adminReply.isEmpty()) {
                request.setAdminReply(adminReply);
            }
            repairRequestRepository.save(request);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái yêu cầu!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy yêu cầu này!");
        }
        return "redirect:/admin/maintenance";
    }
}
