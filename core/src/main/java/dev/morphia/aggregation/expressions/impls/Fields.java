package dev.morphia.aggregation.expressions.impls;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
@SuppressWarnings("unchecked")
public class Fields {
    private final List<PipelineField> fields = new ArrayList<>();
    private Object owner;

    private Fields(Object owner) {
        this.owner = owner;
    }

    /**
     * @param owner the owner
     * @return the new fields
     */
    public static Fields on(Object owner) {
        return new Fields(owner);
    }

    /**
     * @param name       the name
     * @param expression the expression
     * @param <T>        the type of the owner
     * @return the owner
     */
    public <T> T add(String name, Object expression) {
        fields.add(new PipelineField(name, expression));
        return (T) owner;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the fields
     */
    @MorphiaInternal
    public List<PipelineField> fields() {
        return fields;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the number of fields
     */
    @MorphiaInternal
    public int size() {
        return fields.size();
    }

}
