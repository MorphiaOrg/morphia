package org.mongodb.morphia.issue50;


import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.mapping.validation.ConstraintViolationException;
import org.mongodb.morphia.testutil.TestEntity;

import static org.junit.Assert.fail;


public class TestIdTwice extends TestBase {

    @Test
    public final void testRedundantId() {
        try {
            getMorphia().map(A.class);
            fail();
        } catch (ConstraintViolationException expected) {
            // fine
        }
    }

    public static class A extends TestEntity {
        @Id
        private String extraId;
        @Id
        private String broken;
    }

}
