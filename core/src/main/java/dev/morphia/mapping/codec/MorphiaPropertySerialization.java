package dev.morphia.mapping.codec;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.LoadOnly;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.codec.pojo.PropertyModelBuilder;

import org.bson.codecs.pojo.PropertySerialization;

/**
 * Determines if a property should be serialized or not
 */
@SuppressWarnings("removal")
public class MorphiaPropertySerialization implements PropertySerialization {
    private final List<Annotation> annotations;
    private final MorphiaConfig config;
    private final int modifiers;

    /**
     * @param config   the configuration to use
     * @param property the property in question
     */
    public MorphiaPropertySerialization(MorphiaConfig config, PropertyModelBuilder property) {
        this.config = config;
        annotations = property.annotations();
        modifiers = property.modifiers();
    }

    @Override
    public boolean shouldSerialize(@Nullable Object value) {
        if (!config.storeNulls() && value == null) {
            return false;
        }
        if (config.ignoreFinals() && Modifier.isFinal(modifiers)) {
            return false;
        }
        if (!config.storeEmpties()) {
            if (value instanceof Map && ((Map<?, ?>) value).isEmpty()
                    || value instanceof Collection && ((Collection<?>) value).isEmpty()) {
                return false;
            }
        }
        return doesNotHaveAnnotation(LoadOnly.class);
    }

    private boolean doesNotHaveAnnotation(Class<? extends Annotation> annotationClass) {
        return annotations.stream().noneMatch(a -> a.annotationType().equals(annotationClass));
    }
}
