package searchengine.dto.statistics;

import lombok.Data;

@Data
public class TotalStatisticsDto {
    private int sites;
    private int pages;
    private int lemmas;
    private boolean indexing;
}
