package dev.morphia.aggregation.stages;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.Fields;
import dev.morphia.aggregation.expressions.impls.PipelineField;
import dev.morphia.query.ValidationException;
import dev.morphia.sofia.Sofia;

import java.util.ArrayList;
import java.util.List;

import static dev.morphia.aggregation.expressions.Expressions.value;

/**
 * Passes along the documents with the requested fields to the next stage in the pipeline. The specified fields can be existing fields
 * from the input documents or newly computed fields.
 *
 * @aggregation.expression $projection
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
     * @deprecated use {@link #project()}
     */
    @Deprecated(forRemoval = true)
    public static Projection of() {
        return new Projection();
    }

    /**
     * Creates a new stage
     *
     * @return the new stage
     * @since 2.2
     */
    public static Projection project() {
        return new Projection();
    }

    /**
     * Excludes a field.
     *
     * @param name the field name
     * @return this
     */
    public Projection exclude(String name) {
        return exclude(name, value(false));
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
     * @param name  the field name
     * @param value the value expression
     * @return this
     */
    public Projection include(String name, Expression value) {
        if (includes == null) {
            includes = Fields.on(this);
        }
        includes.add(name, value);
        validateProjections();
        return this;
    }

    /**
     * Includes a field.
     *
     * @param name the field name
     * @return this
     */
    public Projection include(String name) {
        return include(name, value(true));
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

    private Projection exclude(String name, Expression value) {
        if (excludes == null) {
            excludes = Fields.on(this);
        }
        excludes.add(name, value);
        validateProjections();
        return this;
    }

    private void validateProjections() {
        if (includes != null && excludes != null) {
            if (excludes.size() > 1 || !"_id".equals(excludes.getFields().get(0).getName())) {
                throw new ValidationException(Sofia.mixedProjections());
            }
        }
    }
}
