package dev.morphia.mapping;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.mapping.codec.pojo.MorphiaModelBuilder;

/**
 * Defines a function to calculate a discriminator value.  This function is only applied if the existing value is the annotation default
 * value.
 */
public abstract class DiscriminatorFunction {
    /**
     * Defines a function to use the class name for the discriminator value
     */
    public static final DiscriminatorFunction className = new DiscriminatorFunction() {
        @Override
        public String compute(final MorphiaModelBuilder builder) {
            return builder.getType().getName();
        }
    };
    public static final DiscriminatorFunction lowerClassName = new DiscriminatorFunction() {
        @Override
        public String compute(final MorphiaModelBuilder builder) {
            return builder.getType().getName().toLowerCase();
        }
    };
    public static final DiscriminatorFunction simpleName = new DiscriminatorFunction() {
        @Override
        public String compute(final MorphiaModelBuilder builder) {
            return builder.getType().getSimpleName();
        }
    };
    public static final DiscriminatorFunction lowerSimpleName = new DiscriminatorFunction() {
        @Override
        public String compute(final MorphiaModelBuilder builder) {
            return builder.getType().getSimpleName().toLowerCase();
        }
    };

    /**
     * Applies the function to the given model to determine the discriminator value
     *
     * @param builder the builder to evaluate
     */
    public final void apply(final MorphiaModelBuilder builder) {
        String discriminator = Mapper.IGNORED_FIELDNAME;
        Entity entity = (Entity) builder.getAnnotation(Entity.class);
        if (entity != null) {
            discriminator = entity.discriminator();
        } else {
            Embedded embedded = (Embedded) builder.getAnnotation(Embedded.class);
            if (embedded != null) {
                discriminator = embedded.discriminator();
            }
        }
        if (discriminator.equals(Mapper.IGNORED_FIELDNAME)) {
            discriminator = compute(builder);
        }

        builder.discriminator(discriminator);
    }

    protected abstract String compute(MorphiaModelBuilder builder);
}
