package dev.morphia.annotations.experimental;

import dev.morphia.annotations.AnnotationBuilder;
import dev.morphia.annotations.Embedded;

/**
 * Creates a pseudo-instance of an {@code @Embedded} annotation.
 *
 * @morphia.experimental
 * @since 2.1
 */
public class EmbeddedBuilder extends AnnotationBuilder<Embedded> implements Embedded {
    /**
     * Creates a new instance
     *
     * @return the new instance
     */
    public static EmbeddedBuilder builder() {
        return new EmbeddedBuilder();
    }

    @Override
    public Class<Embedded> annotationType() {
        return Embedded.class;
    }

    /**
     * Sets the discriminator value
     * @param discriminator the discriminator to use
     * @return this
     */
    public EmbeddedBuilder discriminator(String discriminator) {
        put("discriminator", discriminator);
        return this;
    }

    /**
     * Sets the discriminator key
     * @param discriminatorKey the discriminator key to use
     * @return this
     */
    public EmbeddedBuilder discriminatorKey(String discriminatorKey) {
        put("discriminatorKey", discriminatorKey);
        return this;
    }

    /**
     * Toggles whether or not to use the discriminator
     * @param use true to use the discriminator
     * @return this
     */
    public EmbeddedBuilder useDiscriminator(boolean use) {
        put("useDiscriminator", use);
        return this;
    }

    @Override
    public String value() {
        return get("value");
    }

    @Override
    public boolean useDiscriminator() {
        return get("useDiscriminator");
    }

    @Override
    public String discriminatorKey() {
        return get("discriminatorKey");
    }

    @Override
    public String discriminator() {
        return get("discriminator");
    }
}
