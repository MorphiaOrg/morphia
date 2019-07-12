package dev.morphia.issue50;

import dev.morphia.mapping.Mapper;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.testutil.TestEntity;

public class TestIdTwice extends TestBase {

    @Test(expected = ConstraintViolationException.class)
    public final void shouldThrowExceptionIfThereIsMoreThanOneId() {
        getMapper().map(A.class);
    }

    public static class A extends TestEntity {
        @Id
        private String extraId;
        @Id
        private String broken;
    }

}
