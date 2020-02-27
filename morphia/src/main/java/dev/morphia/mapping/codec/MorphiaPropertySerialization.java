package dev.morphia.mapping.codec;

import dev.morphia.annotations.LoadOnly;
import dev.morphia.annotations.NotSaved;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.codec.pojo.FieldModelBuilder;
import org.bson.codecs.pojo.PropertySerialization;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Determines if a property should be serialized or not
 */
public class MorphiaPropertySerialization implements PropertySerialization {
    private final List<Annotation> annotations;
    private MapperOptions options;
    private int modifiers;

    /**
     * @param options the options to apply
     * @param field   the field in question
     */
    public MorphiaPropertySerialization(final MapperOptions options, final FieldModelBuilder<?> field) {
        this.options = options;
        annotations = field.getAnnotations();
        modifiers = field.getField().getModifiers();
    }

    @Override
    public boolean shouldSerialize(final Object value) {
        if (!options.isStoreNulls() && value == null) {
            return false;
        }
        if (options.isIgnoreFinals() && Modifier.isFinal(modifiers)) {
            return false;
        }
        if (!options.isStoreEmpties()) {
            if (value instanceof Map && ((Map) value).isEmpty()
                || value instanceof Collection && ((Collection) value).isEmpty()) {
                return false;
            }
        }
        return !hasAnnotation(LoadOnly.class) && !hasAnnotation(NotSaved.class);
    }

    private boolean hasAnnotation(final Class<? extends Annotation> annotationClass) {
        return annotations.stream().anyMatch(a -> a.annotationType().equals(annotationClass));
    }
}
