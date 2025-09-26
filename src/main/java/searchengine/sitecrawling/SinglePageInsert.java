package searchengine.sitecrawling;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingSiteService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE)
public class SinglePageInsert {

    private static final Logger logger = LoggerFactory.getLogger(IndexingSiteService.class);
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;

    public void singlePageInsert(Site site, Page page, Pair<List<Lemma>, List<Index>> lemmaAndIndex) {
        try {
            logger.info("Сохранение сайта: {}", site.getName());
            siteRepository.save(site);

            logger.info("Сохранение страницы: {}", page.getPath());
            pageRepository.save(page);

            logger.info("Сохранение индексов: {}", page.getPath());
            indexRepository.saveAll(lemmaAndIndex.getRight());

            logger.info("Сохранение страницы и её метаданных завершилось.");
        } catch (Exception e) {
            logger.error("Ошибка при сохранении страницы {}: {}", page.getPath(), e.getMessage());
        }
    }
}


   