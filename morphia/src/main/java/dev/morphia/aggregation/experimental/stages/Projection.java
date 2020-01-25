package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.AggregationException;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.expressions.impls.Fields;
import dev.morphia.aggregation.experimental.expressions.impls.PipelineField;
import dev.morphia.sofia.Sofia;

import java.util.ArrayList;
import java.util.List;

import static dev.morphia.aggregation.experimental.expressions.Expressions.value;

/**
 * Passes along the documents with the requested fields to the next stage in the pipeline. The specified fields can be existing fields
 * from the input documents or newly computed fields.
 *
 * @mongodb.driver.manual reference/operator/aggregation/projection/ $projection
 */
public class Projection extends Stage {
    private Fields<Projection> includes;
    private Fields<Projection> excludes;
    private boolean suppressId;

    protected Projection() {
        super("$project");
    }

    /**
     * Creates a new stage
     *
     * @return the new stage
     */
    public static Projection of() {
        return new Projection();
    }

    /**
     * Excludes a field.
     *
     * @param name the field name
     * @return this
     */
    public Projection exclude(final String name) {
        exclude(name, value(false));
        return this;
    }

    private void exclude(final String name, final Expression value) {
        if (includes != null) {
            throw new AggregationException(Sofia.mixedModeProjections());
        }
        if (excludes == null) {
            excludes = Fields.on(this);
        }
        excludes.add(name, value);
    }

    /**
     * @return the fields
     * @morphia.internal
     */
    public List<PipelineField> getFields() {
        List<PipelineField> fields = new ArrayList<>();

        if (includes != null) {
            fields.addAll(includes.getFields());
        }
        if (excludes != null) {
            fields.addAll(excludes.getFields());
        }
        if (suppressId) {
            fields.add(new PipelineField("_id", value(false)));
        }
        return fields;
    }


    /**
     * Includes a field.
     *
     * @param name the field name
     * @return this
     */
    public Projection include(final String name) {
        return include(name, value(true));
    }

    /**
     * Includes a field.
     *
     * @param name  the field name
     * @param value the value expression
     * @return this
     */
    public Projection include(final String name, final Expression value) {
        if (excludes != null) {
            throw new AggregationException(Sofia.mixedModeProjections());
        }
        if (includes == null) {
            includes = Fields.on(this);
        }
        return includes.add(name, value);
    }

    /**
     * Suppresses the _id field in the resulting document.
     *
     * @return this
     */
    public Projection suppressId() {
        suppressId = true;
        return this;
    }

}
