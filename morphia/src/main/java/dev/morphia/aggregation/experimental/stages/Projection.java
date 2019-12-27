package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.sofia.Sofia;

import java.util.ArrayList;
import java.util.List;

import static dev.morphia.aggregation.experimental.expressions.Expression.literal;

public class Projection extends Stage {
    private List<ProjectionField> includes;
    private List<ProjectionField> excludes;
    private boolean suppressId;

    protected Projection() {
        super("$project");
    }

    public static Projection of() {
        return new Projection();
    }

    public Projection exclude(String name) {
        exclude(new ProjectionField(name, literal(false)));
        return this;
    }

    private void exclude(final ProjectionField field) {
        if (includes != null) {
            throw new RuntimeException(Sofia.mixedModeProjections());
        }
        if (excludes == null) {
            excludes = new ArrayList<>();
        }
        excludes.add(field);
    }

    public List<ProjectionField> getFields() {
        List<ProjectionField> fields = new ArrayList<>();

        if (includes != null) {
            fields.addAll(includes);
        }
        if (excludes != null) {
            fields.addAll(excludes);
        }
        if (suppressId) {
            fields.add(new ProjectionField("_id", literal(false)));
        }
        return fields;
    }

    public Projection include(final String name, final Expression expression) {
        include(new ProjectionField(name, expression));
        return this;
    }

    public Projection include(String name) {
        include(new ProjectionField(name, literal(true)));
        ;
        return this;
    }

    private void include(final ProjectionField field) {
        if (excludes != null) {
            throw new RuntimeException(Sofia.mixedModeProjections());
        }
        if (includes == null) {
            includes = new ArrayList<>();
        }
        includes.add(field);
    }

    public Projection supressId() {
        suppressId = true;
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static class ProjectionField {
        private String name;
        private Expression value;


        public ProjectionField(final String name, final Expression value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public Expression getValue() {
            return value;
        }
    }
}
