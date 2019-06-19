package dev.morphia.query.validation;

import dev.morphia.mapping.MappedField;
import dev.morphia.query.FilterOperator;
import org.bson.Document;

import java.util.List;

import static dev.morphia.query.FilterOperator.GEO_WITHIN;
import static dev.morphia.query.validation.MappedFieldTypeValidator.isArrayOfNumbers;
import static dev.morphia.query.validation.MappedFieldTypeValidator.isIterableOfNumbers;
import static java.lang.String.format;

/**
 * Supports validation for queries using the {@code FilterOperator.GEO_WITHIN} operator.
 */
public final class GeoWithinOperationValidator extends OperationValidator {
    private static final GeoWithinOperationValidator INSTANCE = new GeoWithinOperationValidator();

    private GeoWithinOperationValidator() {
    }

    /**
     * Get the instance.
     *
     * @return the Singleton instance of this validator
     */
    public static GeoWithinOperationValidator getInstance() {
        return INSTANCE;
    }

    // this could be a lot more rigorous
    private static boolean isValueAValidGeoQuery(final Object value) {
        if (value instanceof Document) {
            String key = ((Document) value).keySet().iterator().next();
            return key.equals("$box") || key.equals("$center") || key.equals("$centerSphere") || key.equals("$polygon");
        }
        return false;
    }

    @Override
    protected FilterOperator getOperator() {
        return GEO_WITHIN;
    }

    @Override
    protected void validate(final MappedField mappedField, final Object value,
                            final List<ValidationFailure> validationFailures) {
        if (!isArrayOfNumbers(mappedField) && !isIterableOfNumbers(mappedField)) {
            validationFailures.add(new ValidationFailure(format("For a $geoWithin operation, if field '%s' is an array or iterable it "
                                                                + "should have numeric values. Instead it had: %s",
                                                                mappedField, mappedField.getSubClass()
                                                               )));

        }
        if (!isValueAValidGeoQuery(value)) {
            validationFailures.add(new ValidationFailure(format("For a $geoWithin operation, the value should be a valid geo query. "
                                                                + "Instead it was: %s", value)));
        }
    }
}
