package dev.morphia.annotations.builders;

import dev.morphia.annotations.Field;
import dev.morphia.utils.IndexType;

/**
 * @morphia.internal
 * @since 2.0
 */
public class FieldBuilder extends AnnotationBuilder<Field> implements Field {
    @Override
    public Class<Field> annotationType() {
        return Field.class;
    }

    @Override
    public IndexType type() {
        return get("type");
    }

    @Override
    public String value() {
        return get("value");
    }

    @Override
    public int weight() {
        return get("weight");
    }

    public FieldBuilder type(IndexType type) {
        put("type", type);
        return this;
    }

    public FieldBuilder value(String value) {
        put("value", value);
        return this;
    }

    public FieldBuilder weight(int weight) {
        put("weight", weight);
        return this;
    }

}
