package org.mongodb.morphia.issue50;

import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.mapping.validation.ConstraintViolationException;
import org.mongodb.morphia.testutil.TestEntity;

public class TestIdTwice extends TestBase {

    @Test(expected = ConstraintViolationException.class)
    public final void shouldThrowExceptionIfThereIsMoreThanOneId() {
        getMorphia().map(A.class);
    }

    public static class A extends TestEntity {
        @Id
        private String extraId;
        @Id
        private String broken;
    }

}
