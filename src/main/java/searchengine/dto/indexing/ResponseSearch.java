package searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
public class ResponseSearch extends ResponseSite{

    private Boolean result;

    private Integer count;

    private List<ResultSearchRequest> data;

}
