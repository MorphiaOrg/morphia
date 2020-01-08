package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.aggregation.experimental.expressions.Fields;
import dev.morphia.aggregation.experimental.expressions.PipelineField;
import dev.morphia.sofia.Sofia;

import java.util.ArrayList;
import java.util.List;

import static dev.morphia.aggregation.experimental.expressions.Expression.literal;

public class Projection extends Stage {
    private Fields<Projection> includes;
    private Fields<Projection> excludes;
    private boolean suppressId;

    protected Projection() {
        super("$project");
    }

    public static Projection of() {
        return new Projection();
    }

    public Projection exclude(String name) {
        exclude(name, literal(false));
        return this;
    }

    public void exclude(final String name, final Expression value) {
        if (includes != null) {
            throw new RuntimeException(Sofia.mixedModeProjections());
        }
        if (excludes == null) {
            excludes = Fields.on(this);
        }
        excludes.add(name, value);
    }

    public List<PipelineField> getFields() {
        List<PipelineField> fields = new ArrayList<>();

        if (includes != null) {
            fields.addAll(includes.getFields());
        }
        if (excludes != null) {
            fields.addAll(excludes.getFields());
        }
        if (suppressId) {
            fields.add(new PipelineField("_id", literal(false)));
        }
        return fields;
    }


    public Projection include(String name) {
        return include(name, literal(true));
    }

    public Projection include(final String name, final Expression value) {
        if (excludes != null) {
            throw new RuntimeException(Sofia.mixedModeProjections());
        }
        if (includes == null) {
            includes = Fields.on(this);
        }
        return includes.add(name, value);
    }

    public Projection supressId() {
        suppressId = true;
        return this;
    }

}
