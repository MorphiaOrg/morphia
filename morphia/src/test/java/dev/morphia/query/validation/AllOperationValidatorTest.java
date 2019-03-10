package dev.morphia.query.validation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static dev.morphia.query.FilterOperator.ALL;
import static dev.morphia.query.FilterOperator.EQUAL;

public class AllOperationValidatorTest {
    @Test
    public void shouldAllowAllOperatorForIterableMapAndArrayValues() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = AllOperationValidator.getInstance().apply(null, ALL, new int[0], validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowAllOperatorForIterableValues() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = AllOperationValidator.getInstance().apply(null, ALL, Collections.emptySet(), validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowAllOperatorForListValues() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = AllOperationValidator.getInstance().apply(null, ALL, asList(1, 2), validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowAllOperatorForMapValues() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = AllOperationValidator.getInstance().apply(null, ALL, new HashMap<String, String>(), validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldGiveAValidationErrorForANullValue() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = AllOperationValidator.getInstance().apply(null, ALL, null, validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
        assertThat(validationFailures.get(0).toString(), containsString("value cannot be null"));
    }

    @Test
    public void shouldGiveAValidationErrorForAValueThatIsNotAnArrayIterableOrMap() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = AllOperationValidator.getInstance().apply(null, ALL, "invalid value", validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
        assertThat(validationFailures.get(0).toString(), containsString("should be an array, an Iterable, or a Map"));
    }

    @Test
    public void shouldNotApplyValidationIfOperatorIsNotAllOperation() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = AllOperationValidator.getInstance().apply(null, EQUAL, "invalid value", validationFailures);

        // then
        assertThat(validationApplied, is(false));
        assertThat(validationFailures.size(), is(0));
    }
}
