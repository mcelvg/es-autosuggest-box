package net.gmbx.typeahead.demo.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.gmbx.typeahead.demo.model.TypeaheadDoc;
import net.gmbx.typeahead.demo.search.SearchQueryBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rest/v1")
public class SuggestionsResource {

    @Autowired
    private SearchQueryBuilder searchQueryBuilder;

    @Autowired
    private TransportClient client;

    private static final ObjectMapper JACKSON = new ObjectMapper();

    @GetMapping(value = "/{text}")
    public String getHits(@PathVariable final String text) throws JsonProcessingException {
        SearchResponse response = client.prepareSearch().get();
        return String.format("query for \"%s\" found %d hits",text, response.getHits().totalHits);

    }

    @GetMapping(value = "/{indexName}/{text}")
    public List<TypeaheadDoc> basicSearch(@PathVariable final String indexName, @PathVariable final String text) {
        return searchQueryBuilder.matchAllQuery(indexName, text);
    }

}
