package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.Room;
import DAJ2EE.demo.entity.ServiceUsage;
import DAJ2EE.demo.repository.RoomRepository;
import DAJ2EE.demo.repository.ServiceRepository;
import DAJ2EE.demo.repository.ServiceUsageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Service
public class ServiceUsageServiceImpl implements ServiceUsageService {

    @Autowired
    private ServiceUsageRepository serviceUsageRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Override
    public List<ServiceUsage> getAllUsage() {
        return serviceUsageRepository.findAll();
    }

    @Override
    public List<ServiceUsage> getUsageByMonthAndYear(Integer month, Integer year) {
        return serviceUsageRepository.findByMonthAndYear(month, year);
    }

    @Override
    public ServiceUsage saveUsage(Long roomId, String serviceName, Integer reading, Integer month, Integer year) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại"));

        DAJ2EE.demo.entity.Service service = serviceRepository.findByName(serviceName)
                .orElseGet(() -> {
                    DAJ2EE.demo.entity.Service newService = new DAJ2EE.demo.entity.Service();
                    newService.setName(serviceName);
                    newService.setServiceName(serviceName);
                    newService.setUnit(serviceName.equalsIgnoreCase("Điện") ? "kWh" : "m3");
                    // Set default prices: Điện = 3500, Nước = 15000
                    if (serviceName.equalsIgnoreCase("Điện")) {
                        newService.setPricePerUnit(new java.math.BigDecimal("3500"));
                        newService.setPricePerUnitAlt(new java.math.BigDecimal("3500"));
                    } else if (serviceName.equalsIgnoreCase("Nước")) {
                        newService.setPricePerUnit(new java.math.BigDecimal("15000"));
                        newService.setPricePerUnitAlt(new java.math.BigDecimal("15000"));
                    } else {
                        newService.setPricePerUnit(java.math.BigDecimal.ZERO);
                        newService.setPricePerUnitAlt(java.math.BigDecimal.ZERO);
                    }
                    return serviceRepository.save(newService);
                });

        Optional<ServiceUsage> existingUsage = serviceUsageRepository
            .findByRoomAndServiceAndMonthAndYear(room, service, month, year);

        ServiceUsage usage;
        if (existingUsage.isPresent()) {
            usage = existingUsage.get();
        } else {
            usage = new ServiceUsage();
            usage.setRoom(room);
            usage.setService(service);
            usage.setMonth(month);
            usage.setYear(year);

            // Find previous usage (to populate old_value)
            Optional<ServiceUsage> previousUsage = serviceUsageRepository
                    .findLatestBefore(room, service, month, year);

            if (previousUsage.isPresent()) {
                usage.setOldValue(previousUsage.get().getNewValue());
            } else {
                usage.setOldValue(0);
            }
            // set reading date and reading year for new record
            usage.setReadingDate(LocalDate.now());
            usage.setReadingYear(usage.getYear());
        }

        // set new_value and mirror into current_reading/previous_reading
        usage.setNewValue(reading);
        usage.setCurrentReading(reading);
        usage.setPreviousReading(usage.getOldValue() == null ? 0 : usage.getOldValue());

        // Validate: chỉ số mới phải >= chỉ số cũ
        int oldVal = usage.getOldValue() == null ? 0 : usage.getOldValue();
        if (reading < oldVal) {
            throw new RuntimeException("Chỉ số " + serviceName + " mới (" + reading + ") không được nhỏ hơn chỉ số cũ (" + oldVal + ")!");
        }

        // compute billing amount = (new - old) * pricePerUnit
        int diff = usage.getNewValue() - oldVal;
        java.math.BigDecimal price = service.getPricePerUnit() == null ? java.math.BigDecimal.ZERO : service.getPricePerUnit();
        java.math.BigDecimal amount = price.multiply(java.math.BigDecimal.valueOf(diff));
        usage.setAmount(amount);

        if (existingUsage.isPresent()) {
            // update existing record
            return serviceUsageRepository.save(usage);
        } else {
            // use native insert to ensure DB NOT NULL columns are filled
                serviceUsageRepository.insertUsage(
                    usage.getMonth(), usage.getNewValue(), usage.getOldValue(),
                    usage.getCurrentReading(), usage.getPreviousReading(),
                    usage.getAmount(),
                    usage.getReadingYear(),
                    java.sql.Date.valueOf(usage.getReadingDate()),
                    usage.getRoom().getId(), usage.getService().getId(), usage.getYear());
            return usage;
        }
    }

    @Override
    public List<ServiceUsage> getUsageByRoom(Long roomId) {
        return serviceUsageRepository.findByRoomId(roomId);
    }

    @Override
    public List<DAJ2EE.demo.entity.Service> getAllServices() {
        return serviceRepository.findAll();
    }

    @Override
    public Integer getLatestReadingBefore(Long roomId, String serviceName, Integer month, Integer year) {
        Room room = roomRepository.findById(roomId).orElse(null);
        DAJ2EE.demo.entity.Service service = serviceRepository.findByName(serviceName).orElse(null);
        
        if (room == null || service == null) return 0;
        
        return serviceUsageRepository.findLatestBefore(room, service, month, year)
                .map(ServiceUsage::getNewValue)
                .orElse(0);
    }
}
