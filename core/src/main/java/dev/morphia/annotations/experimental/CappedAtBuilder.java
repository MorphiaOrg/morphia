package dev.morphia.annotations.experimental;

import dev.morphia.annotations.CappedAt;
import dev.morphia.annotations.builders.AnnotationBuilder;

/**
 * @morphia.internal
 * @since 2.3
 */
public class CappedAtBuilder extends AnnotationBuilder<CappedAt> implements CappedAt {
    /**
     * Creates a new instance
     *
     * @return the new instance
     */
    public static CappedAtBuilder builder() {
        return new CappedAtBuilder();
    }

    @Override
    public Class<CappedAt> annotationType() {
        return CappedAt.class;
    }

    @Override
    public long count() {
        return get("count");
    }

    @Override
    public long value() {
        return get("value");
    }

    /**
     * Sets the count value
     *
     * @param count the count to use
     * @return this
     */
    public CappedAtBuilder count(long count) {
        put("count", count);
        return this;
    }

    /**
     * Sets the value
     *
     * @param value the value to use
     * @return this
     */
    public CappedAtBuilder value(long value) {
        put("value", value);
        return this;
    }
}
