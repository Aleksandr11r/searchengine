package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.ResponseSite;
import searchengine.dto.statistics.StatisticsResponseDto;
import searchengine.services.IndexingSiteService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingSiteService indexingSiteService;

    public ApiController(StatisticsService statisticsService, IndexingSiteService indexingSiteService) {
        this.statisticsService = statisticsService;
        this.indexingSiteService = indexingSiteService;
    }
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponseDto> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    @GetMapping("/startIndexing")
    public ResponseEntity<ResponseSite> startIndexing() {
        return ResponseEntity.ok(indexingSiteService.startIndexing());
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<ResponseSite> stopIndexing() {
        return ResponseEntity.ok(indexingSiteService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<ResponseSite> indexPage(@RequestParam String url) {
        return ResponseEntity.ok(indexingSiteService.indexPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<ResponseSite> search(@RequestParam String query,
                                               @RequestParam(required = false) String site,
                                               @RequestParam(required = false, defaultValue = "0") Integer offset,
                                               @RequestParam(required = false,defaultValue = "20") Integer limit){
        return ResponseEntity.ok(indexingSiteService.systemSearch(query,site,offset,limit));
    }

}




 