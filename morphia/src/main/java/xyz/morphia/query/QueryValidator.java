package xyz.morphia.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.morphia.internal.PathTarget;
import xyz.morphia.mapping.MappedClass;
import xyz.morphia.mapping.MappedField;
import xyz.morphia.mapping.Mapper;
import xyz.morphia.query.validation.AllOperationValidator;
import xyz.morphia.query.validation.DefaultTypeValidator;
import xyz.morphia.query.validation.DoubleTypeValidator;
import xyz.morphia.query.validation.EntityAnnotatedValueValidator;
import xyz.morphia.query.validation.EntityTypeAndIdValueValidator;
import xyz.morphia.query.validation.ExistsOperationValidator;
import xyz.morphia.query.validation.GeoWithinOperationValidator;
import xyz.morphia.query.validation.InOperationValidator;
import xyz.morphia.query.validation.IntegerTypeValidator;
import xyz.morphia.query.validation.KeyValueTypeValidator;
import xyz.morphia.query.validation.ListValueValidator;
import xyz.morphia.query.validation.LongTypeValidator;
import xyz.morphia.query.validation.ModOperationValidator;
import xyz.morphia.query.validation.NotInOperationValidator;
import xyz.morphia.query.validation.PatternValueValidator;
import xyz.morphia.query.validation.SizeOperationValidator;
import xyz.morphia.query.validation.ValidationFailure;

import java.util.List;

import static java.lang.String.format;

final class QueryValidator {
    private QueryValidator() {
    }

    /*package*/
    static boolean isCompatibleForOperator(final MappedClass mappedClass, final MappedField mappedField, final Class<?> type,
                                           final FilterOperator op,
                                           final Object value, final List<ValidationFailure> validationFailures) {
        // TODO: it's really OK to have null values?  I think this is to prevent null pointers further down,
        // but I want to move the null check into the operations that care whether they allow nulls or not.
        if (value == null || type == null) {
            return true;
        }

        boolean validationApplied = ExistsOperationValidator.getInstance().apply(mappedField, op, value, validationFailures)
                                    || SizeOperationValidator.getInstance().apply(mappedField, op, value, validationFailures)
                                    || InOperationValidator.getInstance().apply(mappedField, op, value, validationFailures)
                                    || NotInOperationValidator.getInstance().apply(mappedField, op, value, validationFailures)
                                    || ModOperationValidator.getInstance().apply(mappedField, op, value, validationFailures)
                                    || GeoWithinOperationValidator.getInstance().apply(mappedField, op, value, validationFailures)
                                    || AllOperationValidator.getInstance().apply(mappedField, op, value, validationFailures)
                                    || KeyValueTypeValidator.getInstance().apply(type, value, validationFailures)
                                    || IntegerTypeValidator.getInstance().apply(type, value, validationFailures)
                                    || LongTypeValidator.getInstance().apply(type, value, validationFailures)
                                    || DoubleTypeValidator.getInstance().apply(type, value, validationFailures)
                                    || PatternValueValidator.getInstance().apply(type, value, validationFailures)
                                    || EntityAnnotatedValueValidator.getInstance().apply(type, value, validationFailures)
                                    || ListValueValidator.getInstance().apply(type, value, validationFailures)
                                    || EntityTypeAndIdValueValidator.getInstance()
                                                                    .apply(mappedClass, mappedField, value, validationFailures)
                                    || DefaultTypeValidator.getInstance().apply(type, value, validationFailures);

        return validationApplied && validationFailures.isEmpty();
    }

}
