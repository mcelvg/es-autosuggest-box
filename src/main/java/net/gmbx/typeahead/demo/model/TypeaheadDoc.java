package net.gmbx.typeahead.demo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"title", "display", "variants", "metadata", "duplicate", "alpha", "beta", "delta", "gamma"})
public class TypeaheadDoc {

    private final String title;
    private final String display;
    private final List<String> variants;
    private final List<String> metadata;

    private final boolean duplicate;
    private final double alpha;
    private final double beta;
    private final double gamma;
    private final double delta;

    @JsonCreator
    public TypeaheadDoc(@JsonProperty("title") String title,
                        @JsonProperty("display") String display,
                        @JsonProperty("variants") List<String> variants,
                        @JsonProperty("metadata") List<String> metadata,
                        @JsonProperty("duplicate") boolean duplicate,
                        @JsonProperty("alpha") double alpha,
                        @JsonProperty("beta") double beta,
                        @JsonProperty("gamma") double gamma,
                        @JsonProperty("delta") double delta) {
        this.title = title;
        this.display = display;
        this.variants = variants;
        this.metadata = metadata;
        this.duplicate = duplicate;
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
        this.delta = delta;
    }

    public String getTitle() {
        return title;
    }

    public String getDisplay() {
        return display;
    }

    public List<String> getVariants() {
        return variants;
    }

    public List<String> getMetadata() {
        return metadata;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public double getAlpha() {
        return alpha;
    }

    public double getBeta() {
        return beta;
    }

    public double getGamma() {
        return gamma;
    }

    public double getDelta() {
        return delta;
    }

}
