package dev.morphia.query.validation;

import dev.morphia.Key;

import java.util.List;

import static java.lang.String.format;

/**
 * This makes sure that the field type and the Key type match.
 */
public final class KeyValueTypeValidator extends ValueValidator {
    private static final KeyValueTypeValidator INSTANCE = new KeyValueTypeValidator();

    private KeyValueTypeValidator() {
    }

    /**
     * Get the instance.
     *
     * @return the Singleton instance of this validator
     */
    public static KeyValueTypeValidator getInstance() {
        return INSTANCE;
    }

    @Override
    protected Class<?> getRequiredValueType() {
        return Key.class;
    }

    @Override
    protected void validate(final Class<?> type, final Object value, final List<ValidationFailure> validationFailures) {
        if (!type.equals(((Key) value).getType()) && !type.equals(Key.class)) {
            validationFailures.add(new ValidationFailure(format("When value is a Key, the type needs to be the right kind of class. "
                                                                + "Type was %s and value was '%s'", type, value)
            ));
        }
    }
}

