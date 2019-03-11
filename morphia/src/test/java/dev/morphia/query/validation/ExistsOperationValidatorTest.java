package dev.morphia.query.validation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static dev.morphia.query.FilterOperator.ALL;
import static dev.morphia.query.FilterOperator.EXISTS;

public class ExistsOperationValidatorTest {

    @Test
    public void shouldAllowBooleanValuesForExistsOperator() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = ExistsOperationValidator.getInstance().apply(null, EXISTS, Boolean.TRUE, validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowPrimitiveBooleanValuesForExistsOperator() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = ExistsOperationValidator.getInstance().apply(null, EXISTS, true, validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldNotApplyValidationIfOperatorIsNotExistsOperator() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = ExistsOperationValidator.getInstance().apply(null, ALL, true, validationFailures);

        // then
        assertThat(validationApplied, is(false));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldRejectNonBooleanValuesForExistsOperator() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = ExistsOperationValidator.getInstance().apply(null, EXISTS, "value", validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
    }
}
