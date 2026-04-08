package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.Room;
import DAJ2EE.demo.entity.ServiceUsage;
import DAJ2EE.demo.repository.RoomRepository;
import DAJ2EE.demo.repository.ServiceRepository;
import DAJ2EE.demo.repository.ServiceUsageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Service
public class ServiceUsageServiceImpl implements ServiceUsageService {

    private static final String CLOSED_READING_STATUS = "Đã chốt";

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

        int oldVal = resolveOldValueFromPreviousPeriod(room, service, month, year);

        ServiceUsage usage;
        if (existingUsage.isPresent()) {
            usage = existingUsage.get();
        } else {
            usage = new ServiceUsage();
            usage.setRoom(room);
            usage.setService(service);
            usage.setMonth(month);
            usage.setYear(year);
        }

        usage.setMonth(month);
        usage.setYear(year);
        usage.setOldValue(oldVal);
        usage.setReadingDate(LocalDate.now());
        usage.setReadingYear(year);
        usage.setReadingStatus(CLOSED_READING_STATUS);

        // set new_value and mirror into current_reading/previous_reading
        usage.setNewValue(reading);
        usage.setCurrentReading(reading);
        usage.setPreviousReading(oldVal);

        // Validate: chỉ số mới phải >= chỉ số cũ
        if (reading < oldVal) {
            throw new RuntimeException("Chỉ số mới không được nhỏ hơn chỉ số cũ của tháng trước (" + oldVal + ").");
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
                    usage.getRoom().getId(), usage.getService().getId(), usage.getYear(),
                    usage.getReadingStatus());
            return usage;
        }
    }

    private int resolveOldValueFromPreviousPeriod(Room room,
                                                  DAJ2EE.demo.entity.Service service,
                                                  Integer month,
                                                  Integer year) {
        if (month == null || year == null || month < 1 || month > 12) {
            return 0;
        }

        int previousMonth = month == 1 ? 12 : month - 1;
        int previousYear = month == 1 ? year - 1 : year;

        Optional<ServiceUsage> previousMonthUsage = serviceUsageRepository
                .findByRoomAndServiceAndMonthAndYearAndReadingStatus(
                        room, service, previousMonth, previousYear, CLOSED_READING_STATUS);

        if (previousMonthUsage.isEmpty()) {
            previousMonthUsage = serviceUsageRepository
                .findByRoomAndServiceAndMonthAndYear(room, service, previousMonth, previousYear);
        }

        if (previousMonthUsage.isPresent() && previousMonthUsage.get().getNewValue() != null) {
            return previousMonthUsage.get().getNewValue();
        }

        Optional<Integer> latestClosed = serviceUsageRepository.findLatestClosedBefore(
                room, service, month, year, CLOSED_READING_STATUS, PageRequest.of(0, 1))
                .stream()
                .findFirst()
            .map(ServiceUsage::getNewValue);

        if (latestClosed.isPresent()) {
            return latestClosed.get();
        }

        return serviceUsageRepository.findLatestBefore(room, service, month, year, PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .map(ServiceUsage::getNewValue)
            .orElse(0);
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
        
        if (room == null || service == null) {
            return 0;
        }

        return resolveOldValueFromPreviousPeriod(room, service, month, year);
    }
}
