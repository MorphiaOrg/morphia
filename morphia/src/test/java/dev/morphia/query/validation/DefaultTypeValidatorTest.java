package dev.morphia.query.validation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DefaultTypeValidatorTest {
    @Test
    public void shouldAllowTypesThatAreSuperclasses() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        // when
        boolean validationApplied = DefaultTypeValidator.getInstance().apply(Map.class, new HashMap(), validationFailures);
        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowTypesThatMatchTheClassOfTheValue() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        // expect
        assertThat(DefaultTypeValidator.getInstance().apply(String.class, "some String", validationFailures), is(true));
    }

    @Test
    public void shouldAllowTypesThatTheRealTypeOfTheValue() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        // given
        List<Integer> valueAsList = Arrays.asList(1);
        assertThat(DefaultTypeValidator.getInstance().apply(ArrayList.class, valueAsList, validationFailures), is(true));
    }

    @Test
    public void shouldRejectTypesAndValuesThatDoNotMatch() {
        // given
        List<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        // when
        boolean validationApplied = DefaultTypeValidator.getInstance().apply(String.class, 1, validationFailures);
        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
    }
}
