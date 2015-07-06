package org.mongodb.morphia.mapping.validation.fieldrules;


import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.validation.ConstraintViolationException;
import org.mongodb.morphia.testutil.TestEntity;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class LazyReferenceOnArrayTest extends TestBase {

    @Test(expected = ConstraintViolationException.class)
    public void testLazyRefOnArray() {
        getMorphia().map(LazyOnArray.class);
    }

    public static class R extends TestEntity {
    }

    public static class LazyOnArray extends TestEntity {
        @Reference(lazy = true)
        private R[] r;

    }
}
