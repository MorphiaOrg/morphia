package dev.morphia.annotations.experimental;

import dev.morphia.annotations.CappedAt;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.builders.AnnotationBuilder;

/**
 * Creates a pseudo-instance of an {@code @Entity} annotation.
 *
 * @morphia.experimental
 * @since 2.3
 */
public class EntityBuilder extends AnnotationBuilder<Entity> implements Entity {
    public static EntityBuilder builder() {
        return new EntityBuilder();
    }

    @Override
    public Class<Entity> annotationType() {
        return Entity.class;
    }

    @Override
    public CappedAt cap() {
        return get("cap");
    }

    @Override
    public String concern() {
        return get("concern");
    }

    @Override
    public boolean useDiscriminator() {
        return get("useDiscriminator");
    }

    @Override
    public String value() {
        return get("value");
    }

    @Override
    public String discriminatorKey() {
        return get("discriminatorKey");
    }

    @Override
    public String discriminator() {
        return get("discriminator");
    }

    /**
     * Sets the cap value
     *
     * @param cap the cap to use
     * @return this
     * @since 2.3
     */
    public EntityBuilder cap(CappedAt cap) {
        put("cap", cap);
        return this;
    }

    /**
     * Sets the concern value
     *
     * @param concern the concern to use
     * @return this
     * @since 2.3
     */
    public EntityBuilder concern(String concern) {
        put("concern", concern);
        return this;
    }

    /**
     * Sets the discriminator value
     *
     * @param discriminator the discriminator to use
     * @return this
     * @since 2.3
     */
    public EntityBuilder discriminator(String discriminator) {
        put("discriminator", discriminator);
        return this;
    }

    /**
     * Sets the discriminatorKey value
     *
     * @param key the key to use
     * @return this
     * @since 2.3
     */
    public EntityBuilder discriminatorKey(String key) {
        put("discriminatorKey", key);
        return this;
    }

    /**
     * Sets the useDiscriminator value
     *
     * @param useDiscriminator the useDiscriminator to use
     * @return this
     * @since 2.3
     */
    public EntityBuilder useDiscriminator(boolean useDiscriminator) {
        put("useDiscriminator", useDiscriminator);
        return this;
    }

    /**
     * Sets the collection name to use
     *
     * @param value the value to use
     * @return this
     * @since 2.3
     */
    public EntityBuilder value(String value) {
        put("value", value);
        return this;
    }

}
