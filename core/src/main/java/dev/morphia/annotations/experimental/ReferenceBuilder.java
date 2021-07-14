package dev.morphia.annotations.experimental;

import dev.morphia.annotations.Reference;
import dev.morphia.annotations.builders.AnnotationBuilder;

/**
 * @morphia.internal
 * @since 2.3
 */
public class ReferenceBuilder extends AnnotationBuilder<Reference> implements Reference {
    public static ReferenceBuilder builder() {
        return new ReferenceBuilder();
    }

    @Override
    public Class<Reference> annotationType() {
        return Reference.class;
    }

    @Override
    public boolean idOnly() {
        return get("idOnly");
    }

    @Override
    public boolean ignoreMissing() {
        return get("ignoreMissing");
    }

    @Override
    public boolean lazy() {
        return get("lazy");
    }

    @Override
    public String value() {
        return get("value");
    }

    /**
     * @param idOnly the value for idOnly
     * @return this
     */
    public ReferenceBuilder idOnly(boolean idOnly) {
        put("idOnly", idOnly);
        return this;
    }

    /**
     * @param ignoreMissing the value for ignoreMissing
     * @return this
     */
    public ReferenceBuilder ignoreMissing(boolean ignoreMissing) {
        put("ignoreMissing", ignoreMissing);
        return this;
    }

    /**
     * @param lazy the value for lazy
     * @return this
     */
    public ReferenceBuilder lazy(boolean lazy) {
        put("lazy", lazy);
        return this;
    }

    public ReferenceBuilder value(String value) {
        put("value", value);
        return this;
    }

}
