package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.sofia.Sofia;

import java.util.ArrayList;
import java.util.List;

import static dev.morphia.aggregation.experimental.expressions.Expression.literal;

public class Projection extends Stage {
    private List<PipelineField> includes;
    private List<PipelineField> excludes;
    private boolean suppressId;

    protected Projection() {
        super("$project");
    }

    public static Projection of() {
        return new Projection();
    }

    public Projection exclude(String name) {
        exclude(new PipelineField(name, literal(false)));
        return this;
    }

    private void exclude(final PipelineField field) {
        if (includes != null) {
            throw new RuntimeException(Sofia.mixedModeProjections());
        }
        if (excludes == null) {
            excludes = new ArrayList<>();
        }
        excludes.add(field);
    }

    public List<PipelineField> getFields() {
        List<PipelineField> fields = new ArrayList<>();

        if (includes != null) {
            fields.addAll(includes);
        }
        if (excludes != null) {
            fields.addAll(excludes);
        }
        if (suppressId) {
            fields.add(new PipelineField("_id", literal(false)));
        }
        return fields;
    }

    public Projection include(final String name, final Expression expression) {
        include(new PipelineField(name, expression));
        return this;
    }

    public Projection include(String name) {
        include(new PipelineField(name, literal(true)));
        ;
        return this;
    }

    private void include(final PipelineField field) {
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

}
