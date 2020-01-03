package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.aggregation.experimental.expressions.Fields;

import java.util.List;

import static java.util.Arrays.asList;

public class Bucket extends Stage {
    private Expression groupBy;
    private List<Expression> boundaries;
    private Object defaultValue;
    private Fields<Bucket> output;

    protected Bucket() {
        super("$bucket");
    }

    public static Bucket of() {
        return new Bucket();
    }

    public Bucket boundaries(final Expression... boundaries) {
        this.boundaries = asList(boundaries);
        return this;
    }

    public Bucket defaultValue(final Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public List<Expression> getBoundaries() {
        return boundaries;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Expression getGroupBy() {
        return groupBy;
    }

    public Fields<Bucket> getOutput() {
        return output;
    }

    public Bucket groupBy(final Expression groupBy) {
        this.groupBy = groupBy;
        return this;
    }

    public Bucket outputField(final String name, final Expression value) {
        if(output == null) {
            output = Expression.fields(this);
        }
        return output.add(name, value);
    }
}
