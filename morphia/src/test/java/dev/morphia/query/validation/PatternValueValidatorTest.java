package dev.morphia.query.validation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PatternValueValidatorTest {
    @Test
    public void shouldAllowValueOfPatternWithTypeOfString() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        // when
        boolean validationApplied = PatternValueValidator.getInstance().apply(String.class, Pattern.compile("."), validationFailures);
        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldNotApplyValidationWhenValueIsNotAPattern() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        // when
        boolean validationApplied = PatternValueValidator.getInstance().apply(String.class, ".", validationFailures);
        // then
        assertThat(validationApplied, is(false));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldRejectNonStringTypeWithValueOfPattern() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        // when
        boolean validationApplied = PatternValueValidator.getInstance().apply(Pattern.class, Pattern.compile("."), validationFailures);
        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
    }
}
