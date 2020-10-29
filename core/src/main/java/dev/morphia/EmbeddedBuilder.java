package dev.morphia;

import dev.morphia.annotations.Embedded;

public class EmbeddedBuilder extends AnnotationBuilder<Embedded> implements Embedded {
    public static EmbeddedBuilder builder() {
        return new EmbeddedBuilder();
    }

    @Override
    public Class<Embedded> annotationType() {
        return Embedded.class;
    }

    public EmbeddedBuilder discriminator(String discriminator) {
        put("discriminator", discriminator);
        return this;
    }

    public EmbeddedBuilder discriminatorKey(String discriminatorKey) {
        put("discriminatorKey", discriminatorKey);
        return this;
    }

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
