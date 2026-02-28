package dev.morphia.mapping;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.codec.pojo.EntityModel;

/**
 * The default reflection-based mapper implementation.
 *
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
public class ReflectiveMapper extends AbstractMapper {

    /**
     * Creates a ReflectiveMapper with the given config.
     *
     * @param config the config to use
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ReflectiveMapper(MorphiaConfig config) {
        super(config);
    }

    /**
     * Copy constructor.
     *
     * @param other the original to copy
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ReflectiveMapper(ReflectiveMapper other) {
        super(other);
    }

    @Override
    @Nullable
    public EntityModel mapEntity(@Nullable Class type) {
        if (isMappable(type)) {
            EntityModel model = mappedEntities.get(type.getName());
            return model != null ? model : register(new EntityModel(this, type));
        }
        return null;
    }

    @Override
    public Mapper copy() {
        return new ReflectiveMapper(this);
    }
}
