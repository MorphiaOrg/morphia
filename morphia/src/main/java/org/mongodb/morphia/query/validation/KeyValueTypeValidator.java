package org.mongodb.morphia.query.validation;

import org.mongodb.morphia.Key;

import java.util.List;

import static java.lang.String.format;

public final class KeyValueTypeValidator extends ValueValidator {
    private static final KeyValueTypeValidator INSTANCE = new KeyValueTypeValidator();
    private KeyValueTypeValidator() {
    }

    @Override
    protected void validate(final Class<?> type, final Object value, final List<ValidationFailure> validationFailures) {
        if (!type.equals(((Key) value).getType()) && !type.equals(Key.class)) {
            validationFailures.add(new ValidationFailure(format("When value is a Key, the type needs to be the right kind of class. " 
                                                                + "Type was %s and value was '%s'", type, value)
            ));
        }
    }

    @Override
    protected Class<?> getRequiredValueType() {
        return Key.class;
    }

    /**
     * Get the instance.
     *
     * @return the Singleton instance of this validator
     */
    public static KeyValueTypeValidator getInstance() {
        return INSTANCE;
    }
}

