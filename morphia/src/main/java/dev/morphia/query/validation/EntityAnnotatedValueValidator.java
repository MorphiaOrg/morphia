package dev.morphia.query.validation;

import dev.morphia.Key;
import dev.morphia.annotations.Entity;

import java.util.List;

import static java.lang.String.format;

/**
 * Ensures that a Class is annotated with @Entity.
 *
 * @see Entity
 */
public final class EntityAnnotatedValueValidator extends TypeValidator {
    private static final EntityAnnotatedValueValidator INSTANCE = new EntityAnnotatedValueValidator();

    private EntityAnnotatedValueValidator() {
    }

    /**
     * Get the instance.
     *
     * @return the Singleton instance of this validator
     */
    public static EntityAnnotatedValueValidator getInstance() {
        return INSTANCE;
    }

    @Override
    protected boolean appliesTo(final Class<?> type) {
        return Key.class.equals(type);
    }

    @Override
    protected void validate(final Class<?> type, final Object value, final List<ValidationFailure> validationFailures) {
        if (value.getClass().getAnnotation(Entity.class) == null) {
            validationFailures.add(new ValidationFailure(format("When type is a Key the value should be an annotated entity. "
                                                                + "Value '%s' was a %s", value, value.getClass())));

        }
    }
}
