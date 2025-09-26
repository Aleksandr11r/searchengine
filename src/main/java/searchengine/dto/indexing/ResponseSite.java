package searchengine.dto.indexing;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class ResponseSite {
    private boolean result;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String error;

    public ResponseSite(boolean result) {
        this.result = result;
    }

    public ResponseSite(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public ResponseSite() {

    }
}
