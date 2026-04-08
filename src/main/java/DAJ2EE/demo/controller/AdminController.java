package DAJ2EE.demo.controller;

import DAJ2EE.demo.repository.UserRepository;
import DAJ2EE.demo.service.InvoiceService;
import DAJ2EE.demo.service.MaintenanceRequestService;
import DAJ2EE.demo.service.NotificationRealtimeService;
import DAJ2EE.demo.service.RoomService;
import DAJ2EE.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import DAJ2EE.demo.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller xử lý các trang dành cho Quản trị viên (Admin).
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private MaintenanceRequestService maintenanceRequestService;

    @Autowired
    private DAJ2EE.demo.service.AuditLogService auditLogService;

    @Autowired
    private NotificationRealtimeService notificationRealtimeService;

    /**
     * Hiển thị trang Quản lý Phân quyền.
     * 
     * @param model Đối tượng chứa dữ liệu truyền xuống View.
     * @return Template admin/permissions.html
     */
    @GetMapping("/permissions")
    public String showPermissionsManagement(Model model) {
        // Chỉ hiển thị user đã được Admin phê duyệt (enabled = true)
        model.addAttribute("users", userRepository.findByEnabledTrue());
        model.addAttribute("fullName", "Admin Panel");
        return "admin/permissions";
    }

    /**
     * Xử lý cập nhật vai trò người dùng qua Form truyền thống.
     */
    @PostMapping("/permissions/update-role")
    public String updateRole(@RequestParam Long userId, @RequestParam Long roleId,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserRole(userId, roleId);
            auditLogService.log("Cập nhật Vai trò", "Đã cập nhật vai trò cho người dùng ID: " + userId + " sang Role ID: " + roleId);
                notificationRealtimeService.publishPortalSyncToAdmins(
                    buildPortalSyncPayload(
                        "permission",
                        "role-updated",
                        "Vai trò người dùng đã được cập nhật",
                        userId,
                        List.of("/admin/permissions", "/admin/tenants", "/admin/residents")
                    )
                );
            return "redirect:/admin/permissions?success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/permissions?error";
        }
    }

    /**
     * API cập nhật quyền hạn cụ thể qua AJAX (Toggle Switch).
     */
    @PostMapping("/api/permissions/update")
    @ResponseBody
    public ResponseEntity<?> updatePermission(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            String permissionName = payload.get("permissionName").toString();
            boolean enabled = (boolean) payload.get("enabled");

            userService.updateUserPermission(userId, permissionName, enabled);
            auditLogService.log("Cập nhật Quyền hạn", (enabled ? "Cấp" : "Thu hồi") + " quyền '" + permissionName + "' cho người dùng ID: " + userId);

                notificationRealtimeService.publishPortalSyncToAdmins(
                    buildPortalSyncPayload(
                        "permission",
                        "permission-updated",
                        "Quyền người dùng đã được cập nhật",
                        userId,
                        List.of("/admin/permissions")
                    )
                );

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Cập nhật quyền " + permissionName + " thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * API cập nhật Vai trò (Role) qua AJAX.
     */
    @PostMapping("/api/permissions/update-role")
    @ResponseBody
    public ResponseEntity<?> updateRoleAjax(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            Long roleId = Long.valueOf(payload.get("roleId").toString());

            userService.updateUserRole(userId, roleId);

                notificationRealtimeService.publishPortalSyncToAdmins(
                    buildPortalSyncPayload(
                        "permission",
                        "role-updated",
                        "Vai trò người dùng đã được cập nhật",
                        userId,
                        List.of("/admin/permissions", "/admin/tenants", "/admin/residents")
                    )
                );

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Cập nhật vai trò thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * API: Lấy danh sách các User đang chờ duyệt (enabled = false)
     */
    @GetMapping("/api/users/pending")
    @ResponseBody
    public ResponseEntity<?> getPendingUsers() {
        try {
            List<User> pendingUsers = userRepository.findByEnabledFalse();
            return ResponseEntity.ok(pendingUsers);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * API: Quản trị viên phê duyệt người dùng.
     * Nhận CCCD và cập nhật thông tin người dùng.
     */
    @PostMapping("/api/users/approve/{id}")
    @ResponseBody
    public ResponseEntity<?> approveUser(@PathVariable("id") Long id, @RequestBody Map<String, String> payload) {
        try {
            String cccd = payload.get("cccd");
            String gender = payload.get("gender");
            String hometown = payload.get("hometown");

            if (cccd == null || !cccd.matches("\\d{12}")) {
                throw new IllegalArgumentException("CCCD không hợp lệ. Phải bao gồm đúng 12 chữ số.");
            }
            if (gender == null || gender.isEmpty()) {
                throw new IllegalArgumentException("Giới tính không được để trống.");
            }
            if (hometown == null || hometown.isEmpty()) {
                throw new IllegalArgumentException("Quê quán không được để trống.");
            }

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + id));
            
            // Cập nhật thông tin và phê duyệt
            user.setCccd(cccd);
            user.setGender(gender);
            user.setHometown(hometown);
            user.setEnabled(true);
            user.setStatus(1); // 1 = Active
            
            userRepository.save(user);
            auditLogService.log("Phê duyệt cư dân", "Đã phê duyệt và cập nhật hồ sơ cho cư dân: " + user.getFullName() + " (ID: " + id + ")");

                notificationRealtimeService.publishPortalSyncToUserAndAdmins(
                    id,
                    buildPortalSyncPayload(
                        "user",
                        "approved",
                        "Một cư dân vừa được phê duyệt",
                        id,
                            List.of("/admin/pending-approvals", "/admin/tenants", "/admin/residents", "/admin", "/admin/index", "/tenant")
                    )
                );

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Đã phê duyệt tài khoản và cập nhật hồ sơ cư dân thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * API: Quản trị viên từ chối và xóa người dùng
     */
    @DeleteMapping("/api/users/reject/{id}")
    @ResponseBody
    public ResponseEntity<?> rejectUser(@PathVariable("id") Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + id));
            
            userRepository.delete(user);
            auditLogService.log("Từ chối cư dân", "Đã từ chối và xóa tài khoản đang chờ duyệt: " + user.getFullName() + " (ID: " + id + ")");

                notificationRealtimeService.publishPortalSyncToAdmins(
                    buildPortalSyncPayload(
                        "user",
                        "rejected",
                        "Một hồ sơ cư dân đã bị từ chối",
                        id,
                            List.of("/admin/pending-approvals", "/admin", "/admin/index")
                    )
                );

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Đã từ chối và xóa tài khoản thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Trang Quản lý phê duyệt cư dân.
     */
    @GetMapping("/pending-approvals")
    public String showPendingApprovals(Model model) {
        List<User> pendingUsers = userRepository.findByEnabledFalse();
        model.addAttribute("pendingUsers", pendingUsers);
        model.addAttribute("fullName", "Admin Panel");
        return "admin/pending-approvals";
    }

    /**
     * Trang Dashboard chính của Admin.
     */
    @GetMapping({ "", "/", "/index" })
    public String adminIndex(Model model) {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        model.addAttribute("totalRooms", roomService.getAllRooms().size());
        model.addAttribute("monthlyRevenue", safeAmount(invoiceService.getRevenue(month, year)));
        model.addAttribute("maintenanceRequestCount", maintenanceRequestService.getAllRequests().size());
        model.addAttribute("currentMonthLabel", String.format("%02d/%d", month, year));
        model.addAttribute("fullName", "Admin Panel");
        return "admin/index";
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    /**
     * Trang danh sách cư dân (TENANT) đã được duyệt.
     */
    @GetMapping("/residents")
    public String residents(Model model) {
        List<User> tenants = userRepository.findByRoleName("ROLE_TENANT");
        model.addAttribute("tenants", tenants);
        model.addAttribute("fullName", "Admin Panel");
        return "admin/residents";
    }

    private Map<String, Object> buildPortalSyncPayload(String entity,
                                                       String action,
                                                       String message,
                                                       Long referenceId,
                                                       List<String> targetPages) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("entity", entity);
        payload.put("action", action);
        payload.put("message", message);
        payload.put("referenceId", referenceId);
        payload.put("targetPages", targetPages);
        payload.put("timestamp", System.currentTimeMillis());
        return payload;
    }
}
