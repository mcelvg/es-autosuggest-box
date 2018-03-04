package net.gmbx.typeahead.demo.search;

import net.gmbx.typeahead.demo.model.TypeaheadDoc;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SearchQueryBuilder {

    public List<TypeaheadDoc> matchAllQuery(String indexName, String text) {
        return new ArrayList<>();
    }

}
