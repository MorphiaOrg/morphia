package dev.morphia.query.validation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static dev.morphia.query.FilterOperator.EQUAL;
import static dev.morphia.query.FilterOperator.NOT_IN;

public class NotInOperationValidatorTest {
    @Test
    public void shouldAllowNotInOperatorForArrayListValues() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        ArrayList<Integer> arrayList = new ArrayList<Integer>(asList(1, 2));
        boolean validationApplied = NotInOperationValidator.getInstance().apply(null, NOT_IN, arrayList, validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowNotInOperatorForArrayValues() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = NotInOperationValidator.getInstance().apply(null, NOT_IN, new int[0], validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowNotInOperatorForIterableValues() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = NotInOperationValidator.getInstance().apply(null, NOT_IN, Collections.emptySet(), validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowNotInOperatorForListValues() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = NotInOperationValidator.getInstance().apply(null, NOT_IN, asList(1, 2), validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowNotInOperatorForMapValues() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = NotInOperationValidator.getInstance()
                                                           .apply(null, NOT_IN, new HashMap<String, String>(), validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldNotApplyForOperatorThatIsNotANotInOperator() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = NotInOperationValidator.getInstance().apply(null, EQUAL, "value", validationFailures);

        // then
        assertThat(validationApplied, is(false));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldRejectNullValues() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = NotInOperationValidator.getInstance().apply(null, NOT_IN, null, validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
        assertThat(validationFailures.get(0).toString(), containsString("value cannot be null"));
    }

    @Test
    public void shouldRejectValuesThatAreNotTheCorrectType() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = NotInOperationValidator.getInstance().apply(null, NOT_IN, "value", validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
    }
}
