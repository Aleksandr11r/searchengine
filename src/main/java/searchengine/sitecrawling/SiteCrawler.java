package searchengine.sitecrawling;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.AppConfigProperties;
import searchengine.model.Page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;
    @Slf4j
    public class SiteCrawler extends RecursiveTask<List<Page>> {
        private static String HEAD_URL;
        private final String another_url;
        private final Set<String> visitedUrls;
        private final AppConfigProperties connectionSetting;
        private static final Pattern FILE_PATTERN = Pattern
                .compile(".*\\.(jpg|jpeg|png|gif|bmp|pdf|doc|docx|xls|xlsx|ppt|pptx|zip|rar|tar|gz|7z|mp3|wav|mp4|mkv|avi|mov|sql)$", Pattern.CASE_INSENSITIVE);
        public SiteCrawler(String url, AppConfigProperties connectionSetting) {
            this(url, ConcurrentHashMap.newKeySet(), connectionSetting);
            HEAD_URL = url;
        }
        public SiteCrawler(String url, Set<String> visitedUrls, AppConfigProperties connectionSetting) {
            this.another_url = url;
            this.visitedUrls = visitedUrls;
            this.connectionSetting = connectionSetting;
        }
        public SiteCrawler(String HeadUrl, String another_url, AppConfigProperties connectionSetting) {
            HEAD_URL = HeadUrl;
            this.another_url = another_url;
            this.connectionSetting = connectionSetting;
            this.visitedUrls = ConcurrentHashMap.newKeySet();
        }

        @Override
        public List<Page> compute() {
            Page currentPage = new Page();
            currentPage.setPath((another_url.substring(HEAD_URL.length())));
            List<Page> pages = new ArrayList<>();

            if (visitedUrls.contains(another_url)) {
                return pages;
            }
            if (Thread.currentThread().isInterrupted() || getPool().isShutdown()){
                Thread.currentThread().interrupt();
                return pages;
            }
            visitedUrls.add(another_url);

            List<SiteCrawler> crawler = new ArrayList<>();

            try {
                fetchAndParsePage(currentPage, another_url);
                pages.add(currentPage);

                processLinks(currentPage.getContent(), visitedUrls, crawler);

                collectResults(crawler, pages);

            } catch (IOException e) {
                currentPage.setCode(500);
                currentPage.setContent(e.getMessage().isEmpty() ? "Индексация остановлена пользователем" : e.getMessage());
                pages.add(currentPage);
                if(Thread.currentThread().isInterrupted() || getPool().isShutdown()){
                    Thread.currentThread().interrupt();
                    return pages;
                }
            }
            return pages;
        }

        private void fetchAndParsePage(Page page, String anotherUrl) throws IOException {
            Connection.Response response = Jsoup.connect(anotherUrl)
                    .userAgent(connectionSetting.getUserAgent())
                    .referrer(connectionSetting.getReferrer())
                    .timeout(5000)
                    .ignoreContentType(true)
                    .execute();

            page.setCode(response.statusCode());
            page.setContent(response.body());
        }

        private void processLinks(String сontent, Set<String> visitedUrls, List<SiteCrawler> crawlers) {
            Document document = Jsoup.parse(сontent);
            Elements links = document.select("a");
            for (Element link : links) {
                if(Thread.currentThread().isInterrupted() || getPool().isShutdown()){
                    Thread.currentThread().interrupt();
                    break;
                }
                String href = link.attr("abs:href").trim();
                if(isValidLink(href)) {
                    SiteCrawler crawlerInstance = new SiteCrawler(href, visitedUrls, connectionSetting);
                    crawlerInstance.fork();
                    crawlers.add(crawlerInstance);
                }
            }
        }

        public boolean isValidLink(String urls) {
            return urls.startsWith(HEAD_URL) && !urls.contains("#") && !visitedUrls.contains(urls)
                    && !FILE_PATTERN.matcher(urls).matches();
        }

        private List<Page> collectResults(List<SiteCrawler> crawlers, List<Page> pages) {
            for(SiteCrawler crawler : crawlers) {
                if(Thread.currentThread().isInterrupted() || getPool().isShutdown()){
                    Thread.currentThread().interrupt();
                    break;
                }
                pages.addAll(crawler.join());
            }
            return pages;
        }

        public Page computePage() {
            Page currentPage = new Page(another_url.substring(HEAD_URL.length()));
            try {
                fetchAndParsePage(currentPage, another_url);
            } catch (IOException e) {
                log.info("Недействительный URL: {}", another_url);
                currentPage.setCode(500);
                currentPage.setContent(e.getMessage());
                return currentPage;
                }
            return currentPage;
            }

    }

