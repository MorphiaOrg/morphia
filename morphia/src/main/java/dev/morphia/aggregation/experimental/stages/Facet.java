package dev.morphia.aggregation.experimental.stages;

import java.util.LinkedHashMap;
import java.util.List;

import static java.util.Arrays.asList;

public class Facet extends Stage {
    private LinkedHashMap<String, List<Stage>> fields = new LinkedHashMap<>();
    protected Facet() {
        super("$facet");
    }

    public static Facet of() {
        return new Facet();
    }

    public Facet field(final String name, final Stage... stages) {
        fields.put(name, asList(stages));
        return this;
    }

    public LinkedHashMap<String, List<Stage>> getFields() {
        return fields;
    }
}
