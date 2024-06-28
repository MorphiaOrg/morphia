package dev.morphia;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.pojo.EntityModel;

/**
 * @hidden
 * @morphia.internal
 * @since 3.0
 */
@MorphiaInternal
public interface EntityAware {
    <T extends EntityAware> T entityModel(@Nullable EntityModel model);
}
