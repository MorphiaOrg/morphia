package dev.morphia.query.validation;

import dev.morphia.TestBase;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import org.bson.Document;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.morphia.query.FilterOperator.EQUAL;
import static dev.morphia.query.FilterOperator.GEO_WITHIN;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class GeoWithinOperationValidatorTest extends TestBase {
    @Test
    public void shouldAllowGeoWithinOperatorForGeoEntityWithListOfIntegers() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<>();
        MappedClass mappedClass = getMappedClass(GeoEntity.class);
        MappedField mappedField = mappedClass.getMappedField("list");
        assertThat(GeoWithinOperationValidator.getInstance().apply(mappedField, GEO_WITHIN, new Document("$box", 1),
                                                                   validationFailures), is(true));
    }

    @Test
    public void shouldAllowGeoWithinOperatorWithAllAppropriateTrimmings() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<>();
        MappedClass mappedClass = getMappedClass(GeoEntity.class);
        MappedField mappedField = mappedClass.getMappedField("array");

        // when
        assertThat(GeoWithinOperationValidator.getInstance().apply(mappedField, GEO_WITHIN, new Document("$box", 1),
                                                                   validationFailures), is(true));
    }

    @Test
    public void shouldNotApplyValidationWhenOperatorIsNotGeoWithin() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<>();

        // when
        boolean validationApplied = GeoWithinOperationValidator.getInstance().apply(null, EQUAL, null, validationFailures);

        // then
        assertThat(validationApplied, is(false));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldRejectGeoWithinOperatorWhenMappedFieldIsArrayThatDoesNotContainNumbers() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<>();
        MappedClass mappedClass = getMappedClass(InvalidGeoEntity.class);
        MappedField mappedField = mappedClass.getMappedField("arrayOfStrings");

        // when
        boolean validationApplied = GeoWithinOperationValidator.getInstance().apply(mappedField, GEO_WITHIN, new Document("$box", 1),
                                                                                    validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
        assertThat(validationFailures.get(0).toString(), containsString("is an array or iterable it should have numeric values"));
    }

    @Test
    public void shouldRejectGeoWithinOperatorWhenMappedFieldIsListThatDoesNotContainNumbers() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<>();
        MappedClass mappedClass = getMappedClass(InvalidGeoEntity.class);
        MappedField mappedField = mappedClass.getMappedField("listOfStrings");

        // when
        boolean validationApplied = GeoWithinOperationValidator.getInstance().apply(mappedField,
                                                                                    GEO_WITHIN,
                                                                                    new Document("$box", 1),
                                                                                    validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
        assertThat(validationFailures.get(0).toString(), containsString("is an array or iterable it should have numeric values"));
    }

    @Test
    public void shouldRejectGeoWithinWhenValueDoesNotContainKeyword() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<>();
        MappedClass mappedClass = getMappedClass(GeoEntity.class);
        MappedField mappedField = mappedClass.getMappedField("array");

        // when
        boolean validationApplied = GeoWithinOperationValidator.getInstance().apply(mappedField,
                                                                                    GEO_WITHIN,
                                                                                    new Document("notValidKey", 1),
                                                                                    validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
        assertThat(validationFailures.get(0).toString(),
                   containsString("For a $geoWithin operation, the value should be a valid geo query"));
    }

    @Test
    public void shouldRejectGeoWithinWhenValueIsNotADBObject() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<>();
        MappedClass mappedClass = getMappedClass(GeoEntity.class);
        MappedField mappedField = mappedClass.getMappedField("array");

        // when
        boolean validationApplied = GeoWithinOperationValidator.getInstance().apply(mappedField,
                                                                                    GEO_WITHIN,
                                                                                    "NotAGeoQuery",
                                                                                    validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
        assertThat(validationFailures.get(0).toString(),
                   containsString("For a $geoWithin operation, the value should be a valid geo query"));
    }

    @SuppressWarnings("unused")
    private static class GeoEntity {
        private final int[] array = {1};
        private final List<Integer> list = List.of(1);
    }

    @SuppressWarnings("unused")
    private static class InvalidGeoEntity {
        private final String[] arrayOfStrings = {"1"};
        private final List<String> listOfStrings = List.of("1");
    }
}
