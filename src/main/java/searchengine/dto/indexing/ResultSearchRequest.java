package searchengine.dto.indexing;

public record ResultSearchRequest(
        String site,
        String siteName,
        String uri,
        String title,
        String snippet,
        Double relevance
) {

}
