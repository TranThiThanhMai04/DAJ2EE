package DAJ2EE.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LatestReadingsResponse {
    private Integer lastElecReading;
    private Integer lastWaterReading;
    private java.math.BigDecimal monthlyRent;
    private Double area;
}
