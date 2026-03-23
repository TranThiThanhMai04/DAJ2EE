package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.Room;
import DAJ2EE.demo.entity.RoomStatus;
import DAJ2EE.demo.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/rooms")
@PreAuthorize("hasRole('ADMIN') and hasAuthority('OP_EDIT_ROOM')")
public class RoomManagementController {

    @Autowired
    private RoomService roomService;

    @GetMapping
    public String listRooms(Model model) {
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("fullName", "Admin Panel");
        return "admin/rooms";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("room", new Room());
        model.addAttribute("statuses", RoomStatus.values());
        model.addAttribute("formTitle", "Thêm phòng trọ");
        model.addAttribute("submitUrl", "/admin/rooms");
        model.addAttribute("fullName", "Admin Panel");
        return "admin/room-form";
    }

    @PostMapping
    public String createRoom(@Valid @ModelAttribute("room") Room room,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            attachFormDefaults(model, "Thêm phòng trọ", "/admin/rooms");
            return "admin/room-form";
        }

        try {
            roomService.createRoom(room);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm phòng thành công");
            return "redirect:/admin/rooms";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("roomNumber", "roomNumber", ex.getMessage());
            attachFormDefaults(model, "Thêm phòng trọ", "/admin/rooms");
            return "admin/room-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("room", roomService.getRoomById(id));
        model.addAttribute("statuses", RoomStatus.values());
        model.addAttribute("formTitle", "Cập nhật phòng trọ");
        model.addAttribute("submitUrl", "/admin/rooms/" + id + "/update");
        model.addAttribute("fullName", "Admin Panel");
        return "admin/room-form";
    }

    @PostMapping("/{id}/update")
    public String updateRoom(@PathVariable("id") Long id,
            @Valid @ModelAttribute("room") Room room,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            attachFormDefaults(model, "Cập nhật phòng trọ", "/admin/rooms/" + id + "/update");
            return "admin/room-form";
        }

        try {
            roomService.updateRoom(id, room);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật phòng thành công");
            return "redirect:/admin/rooms";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("roomNumber", "roomNumber", ex.getMessage());
            attachFormDefaults(model, "Cập nhật phòng trọ", "/admin/rooms/" + id + "/update");
            return "admin/room-form";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('OP_DELETE_ROOM')")
    public String deleteRoom(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        roomService.deleteRoom(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa phòng thành công");
        return "redirect:/admin/rooms";
    }

    private void attachFormDefaults(Model model, String formTitle, String submitUrl) {
        model.addAttribute("statuses", RoomStatus.values());
        model.addAttribute("formTitle", formTitle);
        model.addAttribute("submitUrl", submitUrl);
        model.addAttribute("fullName", "Admin Panel");
    }
}
