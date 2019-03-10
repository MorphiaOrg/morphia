package dev.morphia.mapping.validation.classrules;


import dev.morphia.mapping.MappedField;

import java.util.Arrays;
import java.util.List;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class FieldEnumString {
    private final String display;

    /**
     * Creates a FieldEnumString for the given fields
     *
     * @param fields the fields to use
     */
    public FieldEnumString(final MappedField... fields) {
        this(Arrays.asList(fields));
    }

    /**
     * Creates a FieldEnumString for the given fields
     *
     * @param fields the fields to use
     */
    public FieldEnumString(final List<MappedField> fields) {
        final StringBuilder sb = new StringBuilder(128);
        for (final MappedField mappedField : fields) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(mappedField.getNameToStore());
        }
        display = sb.toString();
    }

    @Override
    public String toString() {
        return display;
    }
}
