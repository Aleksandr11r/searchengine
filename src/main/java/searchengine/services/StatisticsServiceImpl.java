package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Sites;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItemDto;
import searchengine.dto.statistics.StatisticsDataDto;
import searchengine.dto.statistics.StatisticsResponseDto;
import searchengine.dto.statistics.TotalStatisticsDto;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final SitesList sites;
    @Override
    public StatisticsResponseDto getStatistics() {

        TotalStatisticsDto total = new TotalStatisticsDto();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItemDto> detailed = new ArrayList<>();
        List<Sites> sitesList = sites.getSites();
        for(int i = 0; i < sitesList.size(); i++) {
            Sites site = sitesList.get(i);
            DetailedStatisticsItemDto item = new DetailedStatisticsItemDto();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            Site sites = siteRepository.findByUrl(site.getUrl());
            int pages = sites == null ? 0 : pageRepository.countPagesToSite(sites.getId());
            int lemmas = sites == null ? 0 : lemmaRepository.countLemmaToSite(sites.getId());
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(sites == null ? "Сайт не проиндексирован" : sites.getStatus().toString());
            item.setError(sites == null ? "Сайт не проиндексирован" : sites.getError());
            item.setStatusTime(sites == null ? 0 : sites.getStatusTime().getLong(ChronoField.MILLI_OF_SECOND));
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponseDto response = new StatisticsResponseDto();
        StatisticsDataDto data = new StatisticsDataDto();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

}
