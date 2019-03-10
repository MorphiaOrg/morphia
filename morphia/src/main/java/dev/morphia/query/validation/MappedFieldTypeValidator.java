package dev.morphia.query.validation;

import dev.morphia.mapping.MappedField;

final class MappedFieldTypeValidator implements Validator {
    private MappedFieldTypeValidator() {
    }

    static boolean isArrayOfNumbers(final MappedField mappedField) {
        Class subClass = mappedField.getSubClass();
        return mappedField.getType().isArray()
               && (subClass == int.class || subClass == long.class || subClass == double.class || subClass == float.class);
    }

    static boolean isIterableOfNumbers(final MappedField mappedField) {
        return Iterable.class.isAssignableFrom(mappedField.getType()) && Number.class.isAssignableFrom(mappedField.getSubClass());
    }
}
