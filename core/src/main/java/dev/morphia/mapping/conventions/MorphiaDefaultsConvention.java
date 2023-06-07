package dev.morphia.mapping.conventions;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.ExternalEntity;
import dev.morphia.annotations.internal.EntityBuilder;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;

/**
 * A set of conventions to apply to Morphia entities
 */
@MorphiaInternal
@SuppressWarnings({ "unchecked", "deprecation" })
public class MorphiaDefaultsConvention implements MorphiaConvention {

    @Override
    public void apply(Mapper mapper, EntityModelBuilder modelBuilder) {
        MorphiaConfig config = mapper.getConfig();

        final Entity entity = modelBuilder.getAnnotation(Entity.class);
        final Embedded embedded = modelBuilder.getAnnotation(Embedded.class);
        final ExternalEntity externalEntity = modelBuilder.getAnnotation(ExternalEntity.class);
        if (entity != null) {
            modelBuilder.enableDiscriminator(entity.useDiscriminator());
            modelBuilder.discriminatorKey(applyDefaults(entity.discriminatorKey(), config.discriminatorKey()));
        } else if (externalEntity != null) {
            modelBuilder.enableDiscriminator(externalEntity.useDiscriminator());
            modelBuilder.discriminatorKey(applyDefaults(externalEntity.discriminatorKey(), config.discriminatorKey()));
            modelBuilder.annotation(EntityBuilder.entityBuilder()
                    .cap(externalEntity.cap())
                    .concern(externalEntity.concern())
                    .discriminator(externalEntity.discriminator())
                    .discriminatorKey(externalEntity.discriminatorKey())
                    .value(externalEntity.value())
                    .useDiscriminator(externalEntity.useDiscriminator())
                    .build());
            modelBuilder.targetType(externalEntity.target());
        } else {
            modelBuilder.enableDiscriminator(embedded == null || embedded.useDiscriminator());
            modelBuilder.discriminatorKey(applyDefaults(embedded != null ? embedded.discriminatorKey() : Mapper.IGNORED_FIELDNAME,
                    config.discriminatorKey()));
        }

        config.discriminator().apply(modelBuilder);
    }

    String applyDefaults(String configured, String defaultValue) {
        if (!configured.equals(Mapper.IGNORED_FIELDNAME)) {
            return configured;
        } else {
            return defaultValue;
        }
    }

}
