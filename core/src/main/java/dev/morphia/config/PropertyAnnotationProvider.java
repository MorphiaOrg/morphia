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
    Property convertToMorphia(T annotation);

    Class<T> provides();
}
