package dev.morphia.annotations.experimental;

import dev.morphia.annotations.Id;
import dev.morphia.annotations.builders.AnnotationBuilder;

/**
 * @morphia.internal
 * @since 2.3
 */
public class IdBuilder extends AnnotationBuilder<Id> implements Id {
    public static IdBuilder builder() {
        return new IdBuilder();
    }

    @Override
    public Class<Id> annotationType() {
        return Id.class;
    }
}
