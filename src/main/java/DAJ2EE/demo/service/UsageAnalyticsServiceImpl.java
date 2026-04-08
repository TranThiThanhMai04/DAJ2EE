package DAJ2EE.demo.service;

import DAJ2EE.demo.dto.UsageTrendDto;
import DAJ2EE.demo.entity.Contract;
import DAJ2EE.demo.entity.ContractStatus;
import DAJ2EE.demo.entity.ServiceUsage;
import DAJ2EE.demo.repository.ContractRepository;
import DAJ2EE.demo.repository.ServiceUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UsageAnalyticsServiceImpl implements UsageAnalyticsService {

    private enum UsageType {
        ELECTRICITY,
        WATER,
        OTHER
    }

    private final ServiceUsageRepository serviceUsageRepository;
    private final ContractRepository contractRepository;

    @Override
    public UsageTrendDto getTenantUsageTrend(Long tenantId) {
        List<ContractStatus> validStatuses = Arrays.asList(ContractStatus.ACTIVE, ContractStatus.PENDING);
        List<Contract> contracts = contractRepository.findByTenantIdAndStatusIn(tenantId, validStatuses);

        if (contracts.isEmpty()) {
            contracts = contractRepository.findByTenantId(tenantId);
        }

        Contract selectedContract = contracts.stream()
                .max(Comparator.comparing(Contract::getId))
                .orElse(null);

        if (selectedContract == null || selectedContract.getRoom() == null) {
            return emptyTrend("TENANT", "N/A");
        }

        Long roomId = selectedContract.getRoom().getId();
        LocalDate now = LocalDate.now();
        List<ServiceUsage> usages = new ArrayList<>();
        usages.addAll(serviceUsageRepository.findByRoomIdAndYear(roomId, now.getYear()));
        usages.addAll(serviceUsageRepository.findByRoomIdAndYear(roomId, now.getYear() - 1));

        return buildTrend("TENANT", selectedContract.getRoom().getRoomNumber(), usages);
    }

    @Override
    public UsageTrendDto getAdminUsageTrend() {
        LocalDate now = LocalDate.now();
        List<ServiceUsage> usages = new ArrayList<>();
        usages.addAll(serviceUsageRepository.findByYear(now.getYear()));
        usages.addAll(serviceUsageRepository.findByYear(now.getYear() - 1));
        return buildTrend("ADMIN", "Tất cả phòng", usages);
    }

    private UsageTrendDto buildTrend(String scope, String roomNumber, List<ServiceUsage> usages) {
        LocalDate now = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(now);
        YearMonth previousMonth = currentMonth.minusMonths(1);
        YearMonth sameMonthLastYear = currentMonth.minusYears(1);

        Map<YearMonth, Integer> electricityByMonth = new HashMap<>();
        Map<YearMonth, Integer> waterByMonth = new HashMap<>();

        for (ServiceUsage usage : usages) {
            if (usage == null || usage.getYear() == null || usage.getMonth() == null) {
                continue;
            }

            YearMonth ym;
            try {
                ym = YearMonth.of(usage.getYear(), usage.getMonth());
            } catch (Exception ignored) {
                continue;
            }

            int consumedUnits = calculateConsumption(usage);
            UsageType usageType = resolveUsageType(usage);

            if (usageType == UsageType.ELECTRICITY) {
                electricityByMonth.merge(ym, consumedUnits, Integer::sum);
            } else if (usageType == UsageType.WATER) {
                waterByMonth.merge(ym, consumedUnits, Integer::sum);
            }
        }

        List<String> labels = new ArrayList<>();
        List<Integer> electricitySeries = new ArrayList<>();
        List<Integer> waterSeries = new ArrayList<>();

        for (int i = 11; i >= 0; i--) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            labels.add(String.format("%02d/%d", targetMonth.getMonthValue(), targetMonth.getYear()));
            electricitySeries.add(electricityByMonth.getOrDefault(targetMonth, 0));
            waterSeries.add(waterByMonth.getOrDefault(targetMonth, 0));
        }

        int electricityCurrent = electricityByMonth.getOrDefault(currentMonth, 0);
        int electricityPrev = electricityByMonth.getOrDefault(previousMonth, 0);
        int electricityLastYear = electricityByMonth.getOrDefault(sameMonthLastYear, 0);

        int waterCurrent = waterByMonth.getOrDefault(currentMonth, 0);
        int waterPrev = waterByMonth.getOrDefault(previousMonth, 0);
        int waterLastYear = waterByMonth.getOrDefault(sameMonthLastYear, 0);

        return UsageTrendDto.builder()
                .scope(scope)
                .roomNumber(roomNumber)
                .labels(labels)
                .electricitySeries(electricitySeries)
                .waterSeries(waterSeries)
                .electricityCurrentMonth(electricityCurrent)
                .electricityPreviousMonth(electricityPrev)
                .electricitySameMonthLastYear(electricityLastYear)
                .electricityVsPreviousPercent(calcPercentChange(electricityCurrent, electricityPrev))
                .electricityVsLastYearPercent(calcPercentChange(electricityCurrent, electricityLastYear))
                .waterCurrentMonth(waterCurrent)
                .waterPreviousMonth(waterPrev)
                .waterSameMonthLastYear(waterLastYear)
                .waterVsPreviousPercent(calcPercentChange(waterCurrent, waterPrev))
                .waterVsLastYearPercent(calcPercentChange(waterCurrent, waterLastYear))
                .build();
    }

    private UsageTrendDto emptyTrend(String scope, String roomNumber) {
        List<String> labels = new ArrayList<>();
        YearMonth currentMonth = YearMonth.from(LocalDate.now());

        for (int i = 11; i >= 0; i--) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            labels.add(String.format("%02d/%d", targetMonth.getMonthValue(), targetMonth.getYear()));
        }

        List<Integer> emptySeries = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            emptySeries.add(0);
        }

        return UsageTrendDto.builder()
                .scope(scope)
                .roomNumber(roomNumber)
                .labels(labels)
                .electricitySeries(new ArrayList<>(emptySeries))
                .waterSeries(new ArrayList<>(emptySeries))
                .electricityCurrentMonth(0)
                .electricityPreviousMonth(0)
                .electricitySameMonthLastYear(0)
                .electricityVsPreviousPercent(0)
                .electricityVsLastYearPercent(0)
                .waterCurrentMonth(0)
                .waterPreviousMonth(0)
                .waterSameMonthLastYear(0)
                .waterVsPreviousPercent(0)
                .waterVsLastYearPercent(0)
                .build();
    }

    private UsageType resolveUsageType(ServiceUsage usage) {
        if (usage == null || usage.getService() == null || usage.getService().getName() == null) {
            return UsageType.OTHER;
        }

        String normalized = normalize(usage.getService().getName());
        if (normalized.contains("dien") || normalized.contains("electric")) {
            return UsageType.ELECTRICITY;
        }
        if (normalized.contains("nuoc") || normalized.contains("water")) {
            return UsageType.WATER;
        }

        return UsageType.OTHER;
    }

    private int calculateConsumption(ServiceUsage usage) {
        Integer newValue = usage.getNewValue() != null ? usage.getNewValue() : usage.getCurrentReading();
        Integer oldValue = usage.getOldValue() != null ? usage.getOldValue() : usage.getPreviousReading();

        int newer = newValue != null ? newValue : 0;
        int older = oldValue != null ? oldValue : 0;
        return Math.max(newer - older, 0);
    }

    private double calcPercentChange(int current, int base) {
        if (base <= 0) {
            return current <= 0 ? 0.0 : 100.0;
        }

        double value = ((double) (current - base) / base) * 100.0;
        return Math.round(value * 10.0) / 10.0;
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
