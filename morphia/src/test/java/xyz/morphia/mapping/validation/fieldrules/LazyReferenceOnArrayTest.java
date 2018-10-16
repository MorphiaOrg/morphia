package xyz.morphia.mapping.validation.fieldrules;


import org.junit.Test;
import xyz.morphia.TestBase;
import xyz.morphia.annotations.Reference;
import xyz.morphia.mapping.validation.ConstraintViolationException;
import xyz.morphia.testutil.TestEntity;


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
