package dev.morphia.mapping.validation.fieldrules;


import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.testutil.TestEntity;


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
