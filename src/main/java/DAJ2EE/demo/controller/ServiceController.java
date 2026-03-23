package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.Room;
import DAJ2EE.demo.entity.ServiceUsage;
import DAJ2EE.demo.service.RoomService;
import DAJ2EE.demo.service.ServiceUsageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/services")
@PreAuthorize("hasRole('ADMIN')")
public class ServiceController {

    @Autowired
    private ServiceUsageService serviceUsageService;

    @Autowired
    private RoomService roomService;

    @GetMapping("")
    public String listUsage(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Model model) {
        
        if (month == null) month = LocalDate.now().getMonthValue();
        if (year == null) year = LocalDate.now().getYear();

        List<Room> rooms = roomService.getAllRooms();
        List<ServiceUsage> usages = serviceUsageService.getUsageByMonthAndYear(month, year);
        
        // Map usage to room for easy access in template
        // Key: roomId + "_" + serviceName
        Map<String, Integer> currentReadings = usages.stream()
                .collect(Collectors.toMap(
                    u -> u.getRoom().getId() + "_" + u.getService().getName(),
                    u -> (u.getNewValue() == null ? 0 : u.getNewValue()),
                    (v1, v2) -> v1 // handle duplicates if any
                ));

        // Compute amounts on-the-fly: (new_value - old_value) * pricePerUnit
        Map<String, java.math.BigDecimal> amounts = usages.stream()
                .collect(Collectors.toMap(
                    u -> u.getRoom().getId() + "_" + u.getService().getName(),
                    u -> {
                        int oldVal = u.getOldValue() == null ? 0 : u.getOldValue();
                        int newVal = u.getNewValue() == null ? oldVal : u.getNewValue();
                        int delta = newVal - oldVal;
                        if (delta < 0) delta = 0;
                        java.math.BigDecimal price = u.getService().getPricePerUnit() == null ? java.math.BigDecimal.ZERO : u.getService().getPricePerUnit();
                        return price.multiply(java.math.BigDecimal.valueOf(delta));
                    },
                    (v1, v2) -> v1
                ));

        model.addAttribute("rooms", rooms);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedYear", year);
        model.addAttribute("readings", currentReadings);
        model.addAttribute("amounts", amounts);
        model.addAttribute("fullName", "Admin Panel");
        
        return "admin/services";
    }

    @PostMapping("/save")
    public String saveUsage(
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam Map<String, String> allParams,
            RedirectAttributes redirectAttributes) {
        
        try {
            for (String key : allParams.keySet()) {
                if (key.startsWith("elec_")) {
                    String value = allParams.get(key);
                    if (value != null && !value.isEmpty()) {
                        Long roomId = Long.parseLong(key.substring(5));
                        Integer reading = Integer.parseInt(value);
                        serviceUsageService.saveUsage(roomId, "Điện", reading, month, year);
                    }
                } else if (key.startsWith("water_")) {
                    String value = allParams.get(key);
                    if (value != null && !value.isEmpty()) {
                        Long roomId = Long.parseLong(key.substring(6));
                        Integer reading = Integer.parseInt(value);
                        serviceUsageService.saveUsage(roomId, "Nước", reading, month, year);
                    }
                }
            }
            redirectAttributes.addFlashAttribute("successMessage", "Lưu chỉ số thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/admin/services?month=" + month + "&year=" + year;
    }
}
