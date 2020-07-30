package dev.morphia.issue50;

import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.validation.ConstraintViolationException;
import org.junit.Test;

public class TestIdTwice extends TestBase {

    @Test(expected = ConstraintViolationException.class)
    public final void shouldThrowExceptionIfThereIsMoreThanOneId() {
        getMapper().map(A.class);
    }

    @Entity
    public static class A {
        @Id
        private String extraId;
        @Id
        private String broken;
    }

}
