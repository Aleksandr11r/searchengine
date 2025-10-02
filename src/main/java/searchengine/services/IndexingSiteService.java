package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Hibernate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.config.AppConfigProperties;
import searchengine.config.Sites;
import searchengine.config.SitesList;
import searchengine.dto.indexing.*;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.sitecrawling.LemmaExtraction;
import searchengine.sitecrawling.SinglePageInsert;
import searchengine.sitecrawling.SiteCrawler;
import searchengine.sitecrawling.SnippetGenerator;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static searchengine.model.SiteStatus.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingSiteService {
    private final AppConfigProperties connectionSetting;
    private final LemmaExtraction lemmaExtraction;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SinglePageInsert singlePageInsert;
    private final SitesList sitesList;
    private final JdbcTemplate jdbcTemplate;
    private ForkJoinPool forkJoinPool;
    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);
    private static final Logger logger = LoggerFactory.getLogger(IndexingSiteService.class);

    public ResponseSite startIndexing() {
        if (indexingInProgress.compareAndSet(false, true)) {
            logger.info("Запуск индексации, значение флага: {}", indexingInProgress);
            forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors(), ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
            for (Sites sitesConfig : sitesList.getSites()) {
                forkJoinPool.submit(() -> indexSite(sitesConfig));
            }
            return new ResponseSite(true);
        }
        else return new ResponseSite(false, "Индексация уже запущена");
    }

    public void indexSite(Sites sitesUrl) {
        Site site;
        if (siteRepository.existsByUrl(sitesUrl.getUrl())) {
            try {
                log.info("Этот сайт уже обрабатывался: {}", sitesUrl);
                site = siteRepository.findByUrl(sitesUrl.getUrl());
                siteRepository.delete(site);
                logger.info("Удаляем сайт из БД {}", sitesUrl.getUrl());
            } catch (Exception e) {
                logger.error("Ошибка удаления {}", e.getMessage());
                throw e;
            }
        }
        logger.info("Создание сайта {}", sitesUrl.getUrl());
        site = createSite(sitesUrl);
        siteRepository.save(site);

        try {
            log.info("Началась индексация сайта: {}", sitesUrl);
            List<Page> pages = new SiteCrawler(sitesUrl.getUrl(), sitesUrl.getUrl(), connectionSetting, indexingInProgress).compute();
            site.setPageList(addSiteToPage(site, pages));

            Pair<List<Lemma>, List<Index>> lemmaAndIndex = findLemmaToText(site, pages);
            site.setLemmaList(lemmaAndIndex.getLeft());

            site.setStatus(forkJoinPool.isShutdown() ? FAILED : INDEXED);
            site.setStatusTime(LocalDateTime.now());
            site.setError(forkJoinPool.isShutdown() ? "Индексация остановлена пользователем" : "");
            
            allInsert(site, pages, lemmaAndIndex);

        } catch (Exception e) {
            log.error("Ошибка при индексация сайта: {}", sitesUrl.getUrl() + " - " + e.getMessage());
            site.setStatus(FAILED);
            site.setStatusTime(LocalDateTime.now());
            site.setError(e.getMessage());
            siteRepository.save(site);
        }
        log.info("Сайт проиндексирован: {}", sitesUrl.getUrl());
    }

    private Site createSite(Sites sitesUrl) {
        Site site = new Site();
        site.setUrl(sitesUrl.getUrl());
        site.setName(sitesUrl.getName());
        site.setStatusTime(LocalDateTime.now());
        site.setError("");
        site.setStatus(INDEXING);
        return site;
    }

    private List<Page> addSiteToPage(Site site, List<Page> pages) {
        pages.forEach(p -> p.setSite(site));
        return pages;
    }

    private Pair<List<Lemma>, List<Index>> findLemmaToText(Site site, List<Page> pages) {
        List<Pair<List<Lemma>, List<Index>>> results = pages.parallelStream()
                .map(page -> findLemmaForSinglePage(page, site)).toList();

        List<Lemma> combinedLemmas = results.stream()
                .flatMap(pair -> pair.getLeft().stream())
                .distinct()
                .collect(Collectors.toList());

        List<Index> combinedIndexes = results.stream()
                .flatMap(pair -> pair.getRight().stream())
                .collect(Collectors.toList());

        return Pair.of(combinedLemmas, combinedIndexes);
    }

    private void allInsert(Site site, List<Page> pages, Pair<List<Lemma>, List<Index>> lemmaAndIndex) {
        forkJoinPool.execute(() -> {
            String string = forkJoinPool.isShutdown() ? String.format("Сохранение сайта %s с остановленной индексацией", site.getName()) : String.format("Сохранение проиндексированного сайта %s", site.getName());
            log.info(string);
            log.info("Сохранение сайта: {}", site.getName());
            siteRepository.save(site);

            log.info("Сохранение страниц: {}", site.getName());
            pageRepository.saveAll(pages);

            log.info("Сохранение лемм: {}", site.getName());
            lemmaRepository.saveAll(lemmaAndIndex.getLeft());

            log.info("Сохранение индексов : {}", site.getName());
            batchIndexInsert(lemmaAndIndex.getRight());
            string = forkJoinPool.isShutdown() ? String.format("Сохранение сайта %s с остановленной индексацией завершено", site.getName()) : String.format("Сохранение проиндексированного сайта %s завершено", site.getName());
            log.info(string);
        });
    }

    private void batchIndexInsert(List<Index> indexList) {
        String sql = "INSERT INTO indexes (page_id,lemma_id,rank) VALUES (?,?,?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Index index = indexList.get(i);
                Hibernate.initialize(index.getPage());
                Hibernate.initialize(index.getLemma());
                ps.setObject(1, index.getPage().getId());
                ps.setObject(2, index.getLemma().getId());
                ps.setFloat(3, index.getRank());
            }
            @Override
            public int getBatchSize() {  //  возвращает размер списка индексов, который соответствует количеству записей, обрабатываемых одним пакетом
                return indexList.size();
            }
        });
    }

    public ResponseSite stopIndexing() {
        if (!forkJoinPool.isShutdown()) {
            forkJoinPool.shutdown();
            indexingInProgress.compareAndSet(true, false);
            log.info("Индексация была остановлена пользователем");
            return new ResponseSite(true);
        } else {
            return new ResponseSite(false, "Индексация не запущена");
        }
    }

    public ResponseSite indexPage(String url) {
        String urlToPage = URLDecoder.decode(url.substring(url.indexOf("h")), StandardCharsets.UTF_8);

        Optional<Sites> siteConfigOptional = checkPageToSiteConfig(urlToPage);
        if (siteConfigOptional.isEmpty()) {
            log.info("сайт {} находится за пределами конфигурационного файла: ", url);
            return new ResponseSite(false, "Данная страница находится за переделами конфигурационных файлов");
        }

        Sites sitesConfig = siteConfigOptional.get();
        log.info("сайт найден в конфигурационном файле: {}", sitesConfig.getName());

        if (checkIndexingPage(urlToPage, sitesConfig)) {
            log.info("Cтраница {} есть в базе данных", urlToPage);
            pageRepository.deletePageByPath(urlToPage.substring(sitesConfig.getUrl().length()));
            log.info("Все данные которые были связаны со страницей: {} удалены", urlToPage);
        }

        Site site = siteRepository.findByUrl(sitesConfig.getUrl());
        if (site == null) {
            site = createSite(sitesConfig);
            log.info("Сайт отсутствует в БД, создался новый сайт: {}", site.getName());
        }

        try {
            Page pages = new SiteCrawler(sitesConfig.getUrl(), urlToPage, connectionSetting,indexingInProgress).computePage();
            log.info("Страница проиндексирована: {}", urlToPage);
            pages.setSite(site);

            Pair<List<Lemma>, List<Index>> lemmaAndIndex = findLemmaForSinglePage(pages, site);
            site.setLemmaList(lemmaAndIndex.getLeft());
            log.info("Найдено лемм: {}", lemmaAndIndex.getLeft().size());
            site.setStatus(INDEXED);
            site.setError("");

            singlePageInsert.singlePageInsert(site, pages, lemmaAndIndex);

        } catch (Exception e) {
            log.error(e.getMessage());
            site.setError(e.getMessage());
            siteRepository.save(site);
        }
        return new ResponseSite(true);
    }

    private Optional<Sites> checkPageToSiteConfig(String url) {
        return sitesList.getSites().stream()
                .filter(sitesConfig -> url.startsWith(sitesConfig.getUrl()))
                .findFirst();
    }

    private boolean checkIndexingPage(String url, Sites sitesConfig) {
        String path = url.substring(sitesConfig.getUrl().length());
        return pageRepository.existsByPath(path);
    }

    private Pair<List<Lemma>, List<Index>> findLemmaForSinglePage(Page page, Site site) {
        Map<String, Lemma> lemmasMap = new HashMap<>();
        List<Index> indexes = new ArrayList<>();

        Map<String, Integer> extractedLemmas = lemmaExtraction.searchLemma(page.getContent());

        for(Map.Entry<String, Integer> entry : extractedLemmas.entrySet()) {
            String lemmaText = entry.getKey();
            int frequency = entry.getValue();

            Lemma lemma = lemmasMap.computeIfAbsent(lemmaText, text -> {
                Lemma newLemma = new Lemma();
                newLemma.setSite(site);
                newLemma.setLemma(text);
                newLemma.setFrequency(frequency);
                return newLemma;
            });

            lemma.setFrequency(lemma.getFrequency() + frequency);

            Index index = new Index();
            index.setPage(page);
            index.setLemma(lemma);
            index.setRank((float) frequency);
            indexes.add(index);
        }
        return Pair.of(new ArrayList<>(lemmasMap.values()), indexes);
    }

    public ResponseSite systemSearch(String query, String siteUrl, Integer offset, Integer limit) {
        if (query.isBlank()) {
            return new ResponseEmptySearchQuery(false, "Пустой поисковый запрос");
        }

        try {
            Site site = siteRepository.findByUrl(siteUrl);
            logger.info("Получен поисковый запрос следующего содержания: {}", query);
            Set<String> uniqueLemma = lemmaExtraction.getLemmaSet(query);
            List<Lemma> filterLemma = calculatingLemmasOnPages(uniqueLemma, site);

            if (filterLemma.isEmpty()) {
                logger.info("Список лемм пуст, по запросу лемм в БД не найдено");
                return new ResponseSearch(true, 0, List.of());
            }

            List<Page> pages = indexRepository.findPagesByLemma(filterLemma.get(0).getLemma());

            for (Lemma lemma : filterLemma) {
                List<Page> pageWithLemma = indexRepository.findPagesByLemma(lemma.getLemma());
                pages = pages.stream()
                        .filter(pageWithLemma::contains)
                        .toList();
            }
            
            List<PageRelevance> resultRelevance = calculatedRelevance(filterLemma);
            resultRelevance.sort(Comparator.comparing(PageRelevance::absoluteRelevance).reversed());

            List<ResultSearchRequest> resultSearchRequestList = createdRequest(resultRelevance, query);

            int totalResultSearchCount = resultSearchRequestList.size();
            List<ResultSearchRequest> paginationResult = resultSearchRequestList.stream()
                    .skip(offset)
                    .limit(limit)
                    .toList();
            return new ResponseSearch(true, totalResultSearchCount, paginationResult);
        } catch (Exception e) {
            log.error("Ошибка при поиске {} :", e.getMessage());
            throw new RuntimeException(e);
        }

    }

    private List<PageRelevance> calculatedRelevance(List<Lemma> filterLemma) {
        List<PageRelevance> resultRelevance = new ArrayList<>();
        Map<Page, Double> pageToRelevance = new HashMap<>();

        List<Integer> lemmaIds = filterLemma.stream()
                .mapToInt(Lemma::getId)
                .boxed()
                .toList();

        List<Index> indexList = indexRepository.findByLemmaIdIn(lemmaIds);
        for (Index index : indexList) {
            Page page = index.getPage();
            double rank = index.getRank();
            pageToRelevance.put(page, pageToRelevance.getOrDefault(page, 0.0) + rank);
        }

        double maxAbsoluteRelevance = pageToRelevance.values().stream()
                .mapToDouble(Double::doubleValue) 
                .max()
                .orElse(0.1);

        for (Map.Entry<Page, Double> entry : pageToRelevance.entrySet()) {
            Page page = entry.getKey();
            double absoluteRelevance = entry.getValue();
            double relativeRelevance = absoluteRelevance / maxAbsoluteRelevance;
            resultRelevance.add(new PageRelevance(page, absoluteRelevance, relativeRelevance));
        }

        return resultRelevance;
    }

    private List<Lemma> calculatingLemmasOnPages(Set<String> lemmas, Site site) {
        long totalPages = pageRepository.count(); 
        double threshold = 0.4;
        Map<String, Lemma> bestLemmas = new HashMap<>();
        

        for (String lemma1 : lemmas) { 
            List<Lemma> lemmaList = site == null ? lemmaRepository.findByLemma(lemma1): lemmaRepository.findByLemmaToSiteId(lemma1, site);
            for (Lemma currentLemma : lemmaList) { 
                if (currentLemma != null) {
                    long countPageToLemma = indexRepository.countPageToLemma(currentLemma.getId()); 
                    double lemmaTotalPages = countPageToLemma / totalPages;
                    if (lemmaTotalPages <= threshold || lemmas.size() < 3) {
                        bestLemmas.merge(
                                currentLemma.getLemma(),
                                currentLemma,
                                (oldValue, newValue) -> oldValue.getFrequency() > newValue.getFrequency() ? oldValue : newValue
                        );
                    }
                }
            }
        }
        return bestLemmas.values().stream().
                sorted(Comparator.comparing(Lemma::getFrequency).reversed()).
                collect(Collectors.toList());
    }

        private List<ResultSearchRequest> createdRequest(List<PageRelevance> pageRelevance, String query) {
        return pageRelevance.stream()
                .map(page -> {
                    String url = page.page().getSite().getUrl();
                    String nameUrl = page.page().getSite().getName();
                    String uri = page.page().getPath();
                    
                    Document document = Jsoup.parse(page.page().getContent());
                    String title = document.title();

                    String snippet = SnippetGenerator.generatedSnippet(query, page.page().getContent());

                    return new ResultSearchRequest(url, nameUrl, uri, title, snippet, page.relativeRelevance());
                }).toList();
    }
}




