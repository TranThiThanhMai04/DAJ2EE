package DAJ2EE.demo.controller;

import DAJ2EE.demo.repository.UserRepository;
import DAJ2EE.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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

    /**
     * Hiển thị trang Quản lý Phân quyền.
     * 
     * @param model Đối tượng chứa dữ liệu truyền xuống View.
     * @return Template admin/permissions.html
     */
    @GetMapping("/permissions")
    public String showPermissionsManagement(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("fullName", "Admin Panel");
        return "admin/permissions";
    }

    /**
     * Xử lý cập nhật vai trò người dùng qua Form truyền thống.
     */
    @PostMapping("/permissions/update-role")
    public String updateRole(@RequestParam Long userId, @RequestParam Long roleId, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserRole(userId, roleId);
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
     * Trang Dashboard chính của Admin.
     */
    @GetMapping({ "", "/", "/index" })
    public String adminIndex(Model model) {
        model.addAttribute("fullName", "Admin Panel");
        return "admin/index";
    }
}