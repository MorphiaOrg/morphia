package org.mongodb.morphia.aggregation;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Projection<T, U> {

    private final String sourceField;
    private final String projectedField;
    private List<Projection> projections;
    private boolean suppressed = false;

    private Projection(final String field) {
        this.sourceField = field;
        projectedField = null;
    }

    private Projection(final String field, final String projectedField) {
        this.sourceField = field;
        this.projectedField = "$" + projectedField;
    }

    private Projection(final String field, final Projection projection, final Projection... subsequent) {
        this(field);
        this.projections = new ArrayList<Projection>();
        projections.add(projection);
        projections.addAll(Arrays.asList(subsequent));
    }

    public static <T, U> Projection<T, U> projection(final String name) {
        return new Projection<T, U>(name);
    }

    public static <T, U> Projection<T, U> projection(final String field, final String projectedField) {
        return new Projection<T, U>(field, projectedField);
    }

    public static <T, U> Projection<T, U> projection(final String field, final Projection projection, final Projection... subsequent) {
        return new Projection<T, U>(field, projection, subsequent);
    }

    public Projection<T, U> suppress() {
        suppressed = true;
        return this;
    }

    public String getProjectedField() {
        return projectedField;
    }

    public String getSourceField() {
        return sourceField;
    }

    public boolean isSuppressed() {
        return suppressed;
    }

    public List<Projection> getProjections() {
        return projections;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Projection{");
        sb.append("projectedField='").append(projectedField).append('\'');
        sb.append(", sourceField='").append(sourceField).append('\'');
        sb.append(", projections=").append(projections);
        sb.append(", suppressed=").append(suppressed);
        sb.append('}');
        return sb.toString();
    }
}
