package searchengine.dto.indexing;

import lombok.Getter;

@Getter
public class ResponseEmptySearchQuery extends ResponseSite{

    private final String error;

    public ResponseEmptySearchQuery(Boolean result, String error) {
        super(result);
        this.error = error;
    }
}