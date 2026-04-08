package DAJ2EE.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageTrendDto {
    private String scope;
    private String roomNumber;

    private List<String> labels;
    private List<Integer> electricitySeries;
    private List<Integer> waterSeries;

    private int electricityCurrentMonth;
    private int electricityPreviousMonth;
    private int electricitySameMonthLastYear;
    private double electricityVsPreviousPercent;
    private double electricityVsLastYearPercent;

    private int waterCurrentMonth;
    private int waterPreviousMonth;
    private int waterSameMonthLastYear;
    private double waterVsPreviousPercent;
    private double waterVsLastYearPercent;
}
