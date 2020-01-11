package dev.morphia.aggregation.experimental.stages;

import com.mongodb.client.model.MergeOptions.WhenMatched;
import com.mongodb.client.model.MergeOptions.WhenNotMatched;
import dev.morphia.aggregation.experimental.expressions.Expression;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class Merge extends Stage {
    private Class type;
    private String database;
    private String collection;
    private List<String> on;
    private Map<String, Expression> variables;
    private WhenMatched whenMatched;
    private List<Stage> whenMatchedPipeline;
    private WhenNotMatched whenNotMatched;

    protected Merge() {
        super("$merge");
    }

    public static Merge merge() {
        return new Merge();
    }

    public String getCollection() {
        return collection;
    }

    public String getDatabase() {
        return database;
    }

    public List<String> getOn() {
        return on;
    }

    public Class getType() {
        return type;
    }

    public Map<String, Expression> getVariables() {
        return variables;
    }

    public WhenMatched getWhenMatched() {
        return whenMatched;
    }

    public List<Stage> getWhenMatchedPipeline() {
        return whenMatchedPipeline;
    }

    public WhenNotMatched getWhenNotMatched() {
        return whenNotMatched;
    }

    public Merge into(final Class type) {
        this.type = type;
        return this;
    }

    public Merge into(final String collection) {
        this.collection = collection;
        return this;
    }

    public Merge into(final String database, final String collection) {
        this.database = database;
        this.collection = collection;
        return this;
    }

    public Merge let(final String variable, final Expression value) {
        if (variables == null) {
            variables = new LinkedHashMap<>();
        }
        variables.put(variable, value);
        return this;
    }

    public Merge on(final String... on) {
        this.on = asList(on);
        return this;
    }

    public Merge whenMatched(final WhenMatched whenMatched) {
        this.whenMatched = whenMatched;
        return this;
    }

    public Merge whenMatched(final List<Stage> pipeline) {
        this.whenMatchedPipeline = pipeline;
        return this;
    }

    public Merge whenNotMatched(final WhenNotMatched whenNotMatched) {
        this.whenNotMatched = whenNotMatched;
        return this;
    }
}
