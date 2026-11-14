package dev.morphia.mapping.codec;

import java.util.Collection;
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
    // all fixed once the property is mapped, so worked out here rather than on every value
    private final boolean storeNulls;
    private final boolean storeEmpties;
    private final boolean neverSerialized;
    private final boolean ignoredFinal;

    /**
     * @param config   the configuration to use
     * @param property the property in question
     */
    public MorphiaPropertySerialization(MorphiaConfig config, PropertyModel property) {
        storeNulls = config.storeNulls();
        storeEmpties = config.storeEmpties();
        ignoredFinal = config.ignoreFinals() && property.isFinal();
        neverSerialized = property.hasAnnotation(LoadOnly.class) || ignoredFinal;
    }

    /**
     * @return true if the property is a final one that's never stored because the configuration ignores finals
     */
    public boolean isIgnoredFinal() {
        return ignoredFinal;
    }

    @Override
    public boolean shouldSerialize(@Nullable Object value) {
        if (neverSerialized) {
            return false;
        }
        if (value == null) {
            return storeNulls;
        }
        return storeEmpties
                || !(value instanceof Map && ((Map<?, ?>) value).isEmpty()
                        || value instanceof Collection && ((Collection<?>) value).isEmpty());
    }
}
