package dev.morphia.config;

import dev.morphia.annotations.Property;
import dev.morphia.annotations.internal.MorphiaExperimental;
import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * This type defines support for a given an annotation as usable to denoting an annotation on an entity and provides a conversion
 * function to map it to a standard Morphia {@link Property} annotation.
 *
 * @param <T> The type of annotation to use to denote a property
 * @since 3.0
 * @morphia.experimental
 * @morphia.internal
 */
@MorphiaInternal
@MorphiaExperimental
public interface PropertyAnnotationProvider<T> {
    /**
     * This method converts external annotations to the morphia form necessary for the mapper to process them.
     * 
     * @param annotation the annotation to convert
     * @return the converted form
     */
    @SuppressWarnings("unused")
    default Property convertToMorphia(T annotation) {
        return (Property) annotation;
    }

    Class<T> provides();
}
