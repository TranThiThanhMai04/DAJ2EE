package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.Contract;
import DAJ2EE.demo.service.ContractService;
import DAJ2EE.demo.service.RoomService;
import DAJ2EE.demo.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/contracts")
public class AdminContractController {

    @Autowired
    private ContractService contractService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private RoomService roomService;

    @GetMapping
    public String listContracts(Model model) {
        model.addAttribute("contracts", contractService.getAllContracts());
        return "admin/contract/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("contract", new Contract());
        model.addAttribute("tenants", tenantService.getAllTenants());
        model.addAttribute("rooms", roomService.getAllRooms());
        return "admin/contract/form";
    }

    @PostMapping("/save")
    public String saveContract(@ModelAttribute("contract") Contract contract, RedirectAttributes redirectAttributes) {
        contractService.saveContract(contract);
        
        // Cập nhật trạng thái phòng thành Đã thuê
        if (contract.getRoom() != null && contract.getRoom().getId() != null) {
            roomService.getRoomById(contract.getRoom().getId()).ifPresent(room -> {
                room.setStatus("Rented");
                roomService.saveRoom(room);
            });
        }
        
        redirectAttributes.addFlashAttribute("successMessage", "Hợp đồng đã được lưu thành công.");
        return "redirect:/admin/contracts";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Contract contract = contractService.getContractById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid contract Id:" + id));
        model.addAttribute("contract", contract);
        model.addAttribute("tenants", tenantService.getAllTenants());
        model.addAttribute("rooms", roomService.getAllRooms());
        return "admin/contract/form";
    }

    @GetMapping("/delete/{id}")
    public String deleteContract(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        contractService.deleteContract(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa hợp đồng.");
        return "redirect:/admin/contracts";
    }
}
