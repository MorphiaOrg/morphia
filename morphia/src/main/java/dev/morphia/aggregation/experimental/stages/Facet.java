package dev.morphia.aggregation.experimental.stages;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * Processes multiple aggregation pipelines within a single stage on the same set of input documents. Each sub-pipeline has its own field
 * in the output document where its results are stored as an array of documents.
 * <p>
 * The $facet stage allows you to create multi-faceted aggregations which characterize data across multiple dimensions, or facets, within
 * a single aggregation stage. Multi-faceted aggregations provide multiple filters and categorizations to guide data browsing and
 * analysis. Retailers commonly use faceting to narrow search results by creating filters on product price, manufacturer, size, etc.
 * <p>
 * Input documents are passed to the $facet stage only once. $facet enables various aggregations on the same set of input documents,
 * without needing to retrieve the input documents multiple times.
 *
 * @aggregation.expression $facet
 */
public class Facet extends Stage {
    private LinkedHashMap<String, List<Stage>> fields = new LinkedHashMap<>();

    protected Facet() {
        super("$facet");
    }

    /**
     * Creates a new facet stage
     * @return the new stage
     */
    public static Facet of() {
        return new Facet();
    }

    /**
     * Adds a field to the facet
     *
     * @param name the field name
     * @param stages the pipeline defining the field
     * @return this
     */
    public Facet field(final String name, final Stage... stages) {
        fields.put(name, asList(stages));
        return this;
    }

    /**
     * @return the fields
     * @morphia.internal
     */
    public Map<String, List<Stage>> getFields() {
        return fields;
    }
}
