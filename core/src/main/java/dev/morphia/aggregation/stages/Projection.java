package dev.morphia.aggregation.stages;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.aggregation.expressions.impls.Fields;
import dev.morphia.aggregation.expressions.impls.PipelineField;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.query.ValidationException;
import dev.morphia.sofia.Sofia;

/**
 * Passes along the documents with the requested fields to the next stage in the pipeline. The specified fields can be existing fields
 * from the input documents or newly computed fields.
 *
 * @aggregation.stage $projection
 */
public class Projection extends Stage {
    private Fields includes;
    private Fields excludes;
    private boolean suppressId;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Projection() {
        super("$project");
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
        return exclude(name, 0);
    }

    /**
     * @return the fields
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public List<PipelineField> getFields() {
        List<PipelineField> fields = new ArrayList<>();

        if (suppressId) {
            fields.add(new PipelineField("_id", 0));
        }
        if (includes != null) {
            fields.addAll(includes.fields());
        }
        if (excludes != null) {
            fields.addAll(excludes.fields());
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
    public Projection include(String name, Object value) {
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
        return include(name, 1);
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

    private Projection exclude(String name, Object value) {
        if (excludes == null) {
            excludes = Fields.on(this);
        }
        excludes.add(name, value);
        validateProjections();
        return this;
    }

    private void validateProjections() {
        if (includes != null && excludes != null) {
            if (excludes.size() > 1 || !"_id".equals(excludes.fields().get(0).name())) {
                throw new ValidationException(Sofia.mixedProjections());
            }
        }
    }
}
