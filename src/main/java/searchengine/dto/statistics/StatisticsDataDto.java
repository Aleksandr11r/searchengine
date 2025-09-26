package searchengine.dto.statistics;

import lombok.Data;

import java.util.List;

@Data
public class StatisticsDataDto {
    private TotalStatisticsDto total;
    private List<DetailedStatisticsItemDto> detailed;
}
