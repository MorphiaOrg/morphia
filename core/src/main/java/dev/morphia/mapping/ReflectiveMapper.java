package dev.morphia.mapping;

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
     * Creates a ReflectiveMapper with the given config and classloader.
     *
     * @param config      the config to use
     * @param classLoader the classloader to use for class and resource resolution
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ReflectiveMapper(MorphiaConfig config, ClassLoader classLoader) {
        super(config, classLoader);
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

    /**
     * Copy constructor that reuses another mapper's entity graph under a different config.
     *
     * @param other  the original to clone the entity graph from
     * @param config the config the new mapper should report/operate under
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ReflectiveMapper(ReflectiveMapper other, MorphiaConfig config) {
        super(other, config);
    }

    @Override
    protected EntityModel buildModel(Class<?> type) {
        return new EntityModel(this, type);
    }

    @Override
    public Mapper copy() {
        return new ReflectiveMapper(this);
    }
}
