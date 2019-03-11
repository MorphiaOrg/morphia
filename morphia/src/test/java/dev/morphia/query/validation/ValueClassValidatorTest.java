package dev.morphia.query.validation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ValueClassValidatorTest {
    @Test
    public void shouldAllowClassesWithExactlyTheSameType() {
        // expect
        assertThat(ValueClassValidator.valueIsClassOrSubclassOf(new Integer(1), Integer.class), is(true));
    }

    @Test
    public void shouldAllowPrimitiveValuesComparedToObjectType() {
        // expect
        assertThat(ValueClassValidator.valueIsClassOrSubclassOf(1, Integer.class), is(true));
    }

    @Test
    public void shouldAllowValueWithClassThatIsSubclassOfType() {
        // expect
        assertThat(ValueClassValidator.valueIsClassOrSubclassOf(new ArrayList(), List.class), is(true));
    }

    @Test
    public void shouldRejectValueThatDoesNotMatchType() {
        // expect
        assertThat(ValueClassValidator.valueIsClassOrSubclassOf(1, String.class), is(false));
    }

}
