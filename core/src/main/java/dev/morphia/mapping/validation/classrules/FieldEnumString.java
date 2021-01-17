package dev.morphia.mapping.validation.classrules;


import dev.morphia.mapping.codec.pojo.PropertyModel;

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
    public FieldEnumString(PropertyModel... fields) {
        this(Arrays.asList(fields));
    }

    /**
     * Creates a FieldEnumString for the given fields
     *
     * @param fields the fields to use
     */
    public FieldEnumString(List<PropertyModel> fields) {
        final StringBuilder sb = new StringBuilder(128);
        for (PropertyModel mappedField : fields) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(mappedField.getMappedName());
        }
        display = sb.toString();
    }

    @Override
    public String toString() {
        return display;
    }
}
