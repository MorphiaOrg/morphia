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
import static dev.morphia.query.FilterOperator.IN;

public class InOperationValidatorTest {
    @Test
    public void shouldAllowInOperatorForArrayListValues() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        ArrayList<Integer> arrayList = new ArrayList<Integer>(asList(1, 2));
        boolean validationApplied = InOperationValidator.getInstance().apply(null, IN, arrayList, validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowInOperatorForArrayValues() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = InOperationValidator.getInstance().apply(null, IN, new int[0], validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowInOperatorForIterableValues() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = InOperationValidator.getInstance().apply(null, IN, Collections.emptySet(), validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowInOperatorForListValues() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = InOperationValidator.getInstance().apply(null, IN, asList(1, 2), validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowInOperatorForMapValues() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = InOperationValidator.getInstance().apply(null, IN, new HashMap<String, String>(), validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldNotApplyForOperatorThatIsNotInOperator() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = InOperationValidator.getInstance().apply(null, EQUAL, "value", validationFailures);

        // then
        assertThat(validationApplied, is(false));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldRejectNullValues() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();

        // when
        boolean validationApplied = InOperationValidator.getInstance().apply(null, IN, null, validationFailures);

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
        boolean validationApplied = InOperationValidator.getInstance().apply(null, IN, "value", validationFailures);

        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
    }

}
