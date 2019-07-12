package dev.morphia.query.validation;

import dev.morphia.TestBase;
import org.hamcrest.Matchers;
import org.junit.Test;
import dev.morphia.entities.EntityWithListsAndArrays;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static dev.morphia.query.FilterOperator.EQUAL;
import static dev.morphia.query.FilterOperator.SIZE;

public class SizeOperationValidatorTest extends TestBase {
    @Test
    public void shouldAllowSizeOperatorForArrayListTypesAndIntegerValues() {
        getMapper().map(EntityWithListsAndArrays.class);
        MappedClass mappedClass = getMapper().getMappedClass(EntityWithListsAndArrays.class);
        MappedField mappedField = mappedClass.getMappedField("arrayListOfIntegers");
        List<ValidationFailure> validationFailures = new ArrayList<>();

        // when
        boolean validationApplied = SizeOperationValidator.getInstance().apply(mappedField, SIZE, 3, validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowSizeOperatorForArrayTypeAndIntValues() {
        getMapper().map(EntityWithListsAndArrays.class);
        MappedClass mappedClass = getMapper().getMappedClass(EntityWithListsAndArrays.class);
        MappedField mappedField = mappedClass.getMappedField("arrayOfInts");
        List<ValidationFailure> validationFailures = new ArrayList<>();

        // when
        boolean validationApplied = SizeOperationValidator.getInstance().apply(mappedField, SIZE, 3, validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowSizeOperatorForArrayTypeAndLongValues() {
        getMapper().map(EntityWithListsAndArrays.class);
        MappedClass mappedClass = getMapper().getMappedClass(EntityWithListsAndArrays.class);
        MappedField mappedField = mappedClass.getMappedField("arrayOfInts");
        List<ValidationFailure> validationFailures = new ArrayList<>();

        // when
        boolean validationApplied = SizeOperationValidator.getInstance().apply(mappedField, SIZE, 3L, validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowSizeOperatorForIterableTypesAndIntegerValues() {
        getMapper().map(EntityWithListsAndArrays.class);
        MappedClass mappedClass = getMapper().getMappedClass(EntityWithListsAndArrays.class);
        MappedField mappedField = mappedClass.getMappedField("setOfIntegers");
        List<ValidationFailure> validationFailures = new ArrayList<>();

        // when
        boolean validationApplied = SizeOperationValidator.getInstance().apply(mappedField, SIZE, 3, validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowSizeOperatorForListTypesAndIntValues() {
        getMapper().map(EntityWithListsAndArrays.class);
        MappedClass mappedClass = getMapper().getMappedClass(EntityWithListsAndArrays.class);
        MappedField mappedField = mappedClass.getMappedField("listOfIntegers");
        List<ValidationFailure> validationFailures = new ArrayList<>();

        // when
        boolean validationApplied = SizeOperationValidator.getInstance().apply(mappedField, SIZE, 3, validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowSizeOperatorForListTypesAndLongValues() {
        getMapper().map(EntityWithListsAndArrays.class);
        MappedClass mappedClass = getMapper().getMappedClass(EntityWithListsAndArrays.class);
        MappedField mappedField = mappedClass.getMappedField("listOfIntegers");
        List<ValidationFailure> validationFailures = new ArrayList<>();

        // when
        boolean validationApplied = SizeOperationValidator.getInstance().apply(mappedField, SIZE, 3L, validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldContainValidationFailuresForBothErrorsWhenTypeIsWrongAndValueIsWrong() {
        getMapper().map(EntityWithListsAndArrays.class);
        MappedClass mappedClass = getMapper().getMappedClass(EntityWithListsAndArrays.class);
        MappedField mappedField = mappedClass.getMappedField("notAnArrayOrList");
        List<ValidationFailure> validationFailures = new ArrayList<>();

        // when
        boolean validationApplied = SizeOperationValidator.getInstance().apply(mappedField, SIZE, "value", validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(2));
    }

    @Test
    public void shouldNotApplyValidationToOperatorThatIsNotSize() {
        getMapper().map(EntityWithListsAndArrays.class);
        MappedClass mappedClass = getMapper().getMappedClass(EntityWithListsAndArrays.class);
        MappedField mappedField = mappedClass.getMappedField("list");
        List<ValidationFailure> validationFailures = new ArrayList<>();

        // when
        boolean validationApplied = SizeOperationValidator.getInstance().apply(mappedField, EQUAL, 1, validationFailures);

        // then
        assertThat(validationApplied, is(false));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldRejectSizeOperatorForNonIntegerValues() {
        getMapper().map(EntityWithListsAndArrays.class);
        MappedClass mappedClass = getMapper().getMappedClass(EntityWithListsAndArrays.class);
        MappedField mappedField = mappedClass.getMappedField("listOfStrings");
        List<ValidationFailure> validationFailures = new ArrayList<>();

        // when
        boolean validationApplied = SizeOperationValidator.getInstance().apply(mappedField, SIZE, "value", validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
        assertThat(validationFailures.get(0).toString(), Matchers.containsString("should be an integer type"));
    }

    @Test
    public void shouldRejectSizeOperatorForNonListTypes() {
        getMapper().map(EntityWithListsAndArrays.class);
        MappedClass mappedClass = getMapper().getMappedClass(EntityWithListsAndArrays.class);
        MappedField mappedField = mappedClass.getMappedField("notAnArrayOrList");
        List<ValidationFailure> validationFailures = new ArrayList<>();

        // when
        boolean validationApplied = SizeOperationValidator.getInstance().apply(mappedField, SIZE, 3, validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
        assertThat(validationFailures.get(0).toString(), containsString("should be a List or array."));
    }
}
