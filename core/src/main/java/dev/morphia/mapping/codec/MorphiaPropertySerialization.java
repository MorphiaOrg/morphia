package dev.morphia.mapping.codec;

import com.mongodb.lang.Nullable;
import dev.morphia.annotations.LoadOnly;
import dev.morphia.annotations.NotSaved;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.codec.pojo.PropertyModelBuilder;
import org.bson.codecs.pojo.PropertySerialization;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Determines if a property should be serialized or not
 */
@SuppressWarnings("removal")
public class MorphiaPropertySerialization implements PropertySerialization {
    private final List<Annotation> annotations;
    private final MapperOptions options;
    private final int modifiers;

    /**
     * @param options  the options to apply
     * @param property the property in question
     */
    public MorphiaPropertySerialization(MapperOptions options, PropertyModelBuilder property) {
        this.options = options;
        annotations = property.annotations();
        modifiers = property.modifiers();
    }

    @Override
    public boolean shouldSerialize(@Nullable Object value) {
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

    private boolean hasAnnotation(Class<? extends Annotation> annotationClass) {
        return annotations.stream().anyMatch(a -> a.annotationType().equals(annotationClass));
    }
}
