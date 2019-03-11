package dev.morphia.query.validation;

import org.bson.types.ObjectId;
import org.junit.Test;
import dev.morphia.Key;
import dev.morphia.entities.SimpleEntity;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class KeyValueTypeValidatorTest {
    @Test
    public void shouldAllowTypeThatMatchesKeyKindWhenValueIsAKey() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        // when
        boolean validationApplied = KeyValueTypeValidator.getInstance().apply(Integer.class,
                                                                              new Key<Number>(Integer.class, "Integer", new ObjectId()),
                                                                              validationFailures);
        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldNotValidateWhenValueIsNotAKey() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        // when
        boolean validationApplied = KeyValueTypeValidator.getInstance().apply(String.class, new SimpleEntity(), validationFailures);
        // then
        assertThat(validationApplied, is(false));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldRejectTypeThatDoesNotMatchKeyKindWhenValueIsAKey() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
        // when
        boolean validationApplied = KeyValueTypeValidator.getInstance().apply(String.class,
                                                                              new Key<Number>(Integer.class, "Integer", new ObjectId()),
                                                                              validationFailures);
        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
    }

}
