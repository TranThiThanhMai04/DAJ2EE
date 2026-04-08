package DAJ2EE.demo.controller;

import DAJ2EE.demo.dto.ContractRequestDto;
import DAJ2EE.demo.entity.RoomStatus;
import DAJ2EE.demo.service.ContractService;
import DAJ2EE.demo.service.RoomService;
import DAJ2EE.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/contracts")
@PreAuthorize("hasRole('ADMIN')")
public class ContractController {

    @Autowired
    private ContractService contractService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String listContracts(Model model) {
        model.addAttribute("contracts", contractService.getAllContracts());
        return "admin/contracts";
    }

    @GetMapping("/create")
    public String showCreateForm(@RequestParam(name = "tenantId", required = false) Long tenantId, Model model) {
        ContractRequestDto dto = new ContractRequestDto();
        if (tenantId != null) {
            dto.setTenantId(tenantId);
        }
        model.addAttribute("contractDto", dto);
        // Lấy tất cả các phòng đang trống để hiển thị
        model.addAttribute("rooms", roomService.getAllRooms().stream()
                .filter(room -> room.getStatus() == RoomStatus.EMPTY)
                .toList());
        // Lấy danh sách user (người thuê). Ở đây, ta lấy tất cả user hoặc lọc user có role TENANT nếu có.
        // Giả sử lấy tất cả user làm người thuê.
        model.addAttribute("tenants", userService.getAllUsers());
        return "admin/contract-form";
    }

    @PostMapping("/create")
    public String createContract(@Valid @ModelAttribute("contractDto") ContractRequestDto dto,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("rooms", roomService.getAllRooms().stream()
                    .filter(room -> room.getStatus() == RoomStatus.EMPTY)
                    .toList());
            model.addAttribute("tenants", userService.getAllUsers());
            return "admin/contract-form";
        }

        try {
            contractService.createContract(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo hợp đồng thành công!");
            return "redirect:/admin/contracts";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("rooms", roomService.getAllRooms().stream()
                    .filter(room -> room.getStatus() == RoomStatus.EMPTY)
                    .toList());
            model.addAttribute("tenants", userService.getAllUsers());
            return "admin/contract-form";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Không thể tạo hợp đồng do lỗi hệ thống: " + e.getMessage());
            model.addAttribute("rooms", roomService.getAllRooms().stream()
                    .filter(room -> room.getStatus() == RoomStatus.EMPTY)
                    .toList());
            model.addAttribute("tenants", userService.getAllUsers());
            return "admin/contract-form";
        }
    }

    @PostMapping("/terminate/{id}")
    public String terminateContract(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            contractService.terminateContract(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã kết thúc hợp đồng thành công. Trạng thái phòng đã trở lại TRỐNG.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/contracts";
    }

    @GetMapping("/{id}")
    public String viewContractDetail(@PathVariable("id") Long id, org.springframework.ui.Model model) {
        DAJ2EE.demo.entity.Contract contract = contractService.getContractById(id);
        model.addAttribute("contract", contract);
        return "admin/contract-detail";
    }
}
