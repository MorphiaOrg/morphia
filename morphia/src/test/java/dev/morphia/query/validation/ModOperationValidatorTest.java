package dev.morphia.query.validation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static dev.morphia.query.FilterOperator.EQUAL;
import static dev.morphia.query.FilterOperator.MOD;

public class ModOperationValidatorTest {
    @Test
    public void shouldAllowModOperatorForArrayOfTwoIntegerValues() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = ModOperationValidator.getInstance().apply(null, MOD, new int[2], validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldNotApplyValidationWithANonModOperator() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = ModOperationValidator.getInstance().apply(null, EQUAL, new int[2], validationFailures);

        // then
        assertThat(validationApplied, is(false));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldNotErrorIfModOperatorIsUsedWithZeroLengthArrayOfIntegerValues() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = ModOperationValidator.getInstance().apply(null, MOD, new int[0], validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
        assertThat(validationFailures.get(0).toString(), containsString("should be an array with two integer elements"));
    }

    @Test
    public void shouldRejectModOperatorWithNonArrayValue() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = ModOperationValidator.getInstance().apply(null, MOD, "Not an array", validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
        assertThat(validationFailures.get(0).toString(), containsString("should be an integer array"));
    }

    @Test
    public void shouldRejectModOperatorWithNonIntegerArray() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = ModOperationValidator.getInstance().apply(null, MOD, new String[]{"1", "2"}, validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
        assertThat(validationFailures.get(0).toString(), containsString("Array value needs to contain integers for $mod"));
    }

    @Test
    public void shouldRejectNullValues() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = ModOperationValidator.getInstance().apply(null, MOD, null, validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
        assertThat(validationFailures.get(0).toString(), containsString("value cannot be null"));
    }

}
