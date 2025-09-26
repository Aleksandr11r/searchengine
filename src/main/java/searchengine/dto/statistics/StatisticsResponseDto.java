package searchengine.dto.statistics;

import lombok.Data;

@Data
public class StatisticsResponseDto {
    private boolean result;
    private StatisticsDataDto statistics;
}
