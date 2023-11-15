package dev.morphia.mapping.codec;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.LoadOnly;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.codec.pojo.PropertyModel;

import org.bson.codecs.pojo.PropertySerialization;

/**
 * Determines if a property should be serialized or not
 */
public class MorphiaPropertySerialization implements PropertySerialization<Object> {
    private final List<Annotation> annotations;
    private final MorphiaConfig config;

    private final PropertyModel property;

    /**
     * @param config   the configuration to use
     * @param property the property in question
     */
    public MorphiaPropertySerialization(MorphiaConfig config, PropertyModel property) {
        this.config = config;
        annotations = property.getAnnotations();
        this.property = property;
    }

    @Override
    public boolean shouldSerialize(@Nullable Object value) {
        if (!config.storeNulls() && value == null) {
            return false;
        }
        if (config.ignoreFinals() && property.isFinal()) {
            return false;
        }
        if (!config.storeEmpties()) {
            if (value instanceof Map && ((Map<?, ?>) value).isEmpty()
                    || value instanceof Collection && ((Collection<?>) value).isEmpty()) {
                return false;
            }
        }
        return isNotLoadOnly();
    }

    private boolean isNotLoadOnly() {
        return annotations.stream().noneMatch(a -> a.annotationType().equals(LoadOnly.class));
    }
}
