package dev.morphia.query.validation;

import com.mongodb.BasicDBObject;
import org.junit.Test;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static dev.morphia.query.FilterOperator.EQUAL;
import static dev.morphia.query.FilterOperator.GEO_WITHIN;

public class GeoWithinOperationValidatorTest {
    @Test
    public void shouldAllowGeoWithinOperatorForGeoEntityWithListOfIntegers() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        MappedClass mappedClass = new MappedClass(GeoEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("list");
        assertThat(GeoWithinOperationValidator.getInstance().apply(mappedField, GEO_WITHIN, new BasicDBObject("$box", 1),
                                                                   validationFailures), is(true));
    }

    @Test
    public void shouldAllowGeoWithinOperatorWithAllAppropriateTrimmings() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        MappedClass mappedClass = new MappedClass(GeoEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("array");

        // when
        assertThat(GeoWithinOperationValidator.getInstance().apply(mappedField, GEO_WITHIN, new BasicDBObject("$box", 1),
                                                                   validationFailures), is(true));
    }

    @Test
    public void shouldNotApplyValidationWhenOperatorIsNotGeoWithin() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = GeoWithinOperationValidator.getInstance().apply(null, EQUAL, null, validationFailures);

        // then
        assertThat(validationApplied, is(false));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldRejectGeoWithinOperatorWhenMappedFieldIsArrayThatDoesNotContainNumbers() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        MappedClass mappedClass = new MappedClass(InvalidGeoEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("arrayOfStrings");

        // when
        boolean validationApplied = GeoWithinOperationValidator.getInstance().apply(mappedField, GEO_WITHIN, new BasicDBObject("$box", 1),
                                                                                    validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
        assertThat(validationFailures.get(0).toString(), containsString("is an array or iterable it should have numeric values"));
    }

    @Test
    public void shouldRejectGeoWithinOperatorWhenMappedFieldIsListThatDoesNotContainNumbers() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        MappedClass mappedClass = new MappedClass(InvalidGeoEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("listOfStrings");

        // when
        boolean validationApplied = GeoWithinOperationValidator.getInstance().apply(mappedField,
                                                                                    GEO_WITHIN,
                                                                                    new BasicDBObject("$box", 1),
                                                                                    validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
        assertThat(validationFailures.get(0).toString(), containsString("is an array or iterable it should have numeric values"));
    }

    @Test
    public void shouldRejectGeoWithinWhenValueDoesNotContainKeyword() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        MappedClass mappedClass = new MappedClass(GeoEntity.class, new Mapper());
        MappedField mappedField = mappedClass.getMappedField("array");

        // when
        boolean validationApplied = GeoWithinOperationValidator.getInstance().apply(mappedField,
                                                                                    GEO_WITHIN,
                                                                                    new BasicDBObject("notValidKey", 1),
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
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        MappedClass mappedClass = new MappedClass(GeoEntity.class, new Mapper());
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
        private final List<Integer> list = Arrays.asList(1);
    }

    @SuppressWarnings("unused")
    private static class InvalidGeoEntity {
        private final String[] arrayOfStrings = {"1"};
        private final List<String> listOfStrings = Arrays.asList("1");
    }
}
