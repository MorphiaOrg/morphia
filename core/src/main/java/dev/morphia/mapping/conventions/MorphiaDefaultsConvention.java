package dev.morphia.mapping.conventions;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.ExternalEntity;
import dev.morphia.annotations.internal.EntityBuilder;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;

/**
 * A set of conventions to apply to Morphia entities
 */
@MorphiaInternal
public class MorphiaDefaultsConvention implements MorphiaConvention {

    @Override
    public void apply(Mapper mapper, EntityModel model) {
        MorphiaConfig config = mapper.getConfig();

        Entity entity = model.getAnnotation(Entity.class);
        final ExternalEntity externalEntity = model.getAnnotation(ExternalEntity.class);
        if (entity != null) {
            model.discriminatorEnabled(entity.useDiscriminator());
            model.discriminatorKey(applyDefaults(entity.discriminatorKey(), config.discriminatorKey()));
        } else if (externalEntity != null) {
            model.discriminatorEnabled(externalEntity.useDiscriminator());
            model.discriminatorKey(applyDefaults(externalEntity.discriminatorKey(), config.discriminatorKey()));
            entity = EntityBuilder.entityBuilder()
                    .cap(externalEntity.cap())
                    .concern(externalEntity.concern())
                    .discriminator(externalEntity.discriminator())
                    .discriminatorKey(externalEntity.discriminatorKey())
                    .value(externalEntity.value())
                    .useDiscriminator(externalEntity.useDiscriminator())
                    .build();
            model.annotation(entity);
        }

        model.discriminator(config.discriminator().apply(model.getType().getSimpleName(), entity.discriminator()));
    }

    public static String applyDefaults(String configured, String defaultValue) {
        if (!configured.equals(Mapper.IGNORED_FIELDNAME)) {
            return configured;
        } else {
            return defaultValue;
        }
    }
}
