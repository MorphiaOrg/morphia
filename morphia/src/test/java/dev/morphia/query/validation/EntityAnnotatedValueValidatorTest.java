package dev.morphia.query.validation;

import org.junit.Test;
import dev.morphia.Key;
import dev.morphia.entities.SimpleEntity;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EntityAnnotatedValueValidatorTest {
    @Test
    public void shouldAllowValueWithEntityAnnotationAndTypeOfKey() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        // when
        boolean validationApplied = EntityAnnotatedValueValidator.getInstance().apply(Key.class, new SimpleEntity(), validationFailures);
        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldNotValidateValueWithEntityAnnotationAndNonKeyType() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        // when
        boolean validationApplied = EntityAnnotatedValueValidator.getInstance().apply(String.class, new SimpleEntity(), validationFailures);
        // then
        assertThat(validationApplied, is(false));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldRejectValueWithoutEntityAnnotationAndTypeOfKey() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        // when
        boolean validationApplied = EntityAnnotatedValueValidator.getInstance().apply(Key.class, "value", validationFailures);
        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
    }
}
