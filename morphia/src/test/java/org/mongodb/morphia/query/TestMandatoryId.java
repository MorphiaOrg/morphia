package org.mongodb.morphia.query;


import org.junit.Test;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.mapping.validation.ConstraintViolationException;


public class TestMandatoryId extends TestBase {

    @Entity
    public static class E {
        // not id here
        private String foo = "bar";
    }

    @Test(expected = ConstraintViolationException.class)
    public final void testMissingId() {
        getMorphia().map(E.class);
    }

    @Test(expected = ValidationException.class)
    public final void testMissingIdNoImplicitMapCall() {
        final Key<E> save = getDs().save(new E());

        getDs().getByKey(E.class, save);
    }

}
