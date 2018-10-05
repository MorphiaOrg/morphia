package xyz.morphia.mapping.validation.fieldrules;


import org.junit.Test;
import xyz.morphia.TestBase;
import xyz.morphia.annotations.Reference;
import xyz.morphia.annotations.Serialized;
import xyz.morphia.mapping.validation.ConstraintViolationException;
import xyz.morphia.testutil.TestEntity;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ReferenceAndSerializableTest extends TestBase {
    @Test(expected = ConstraintViolationException.class)
    public void testCheck() {
        getMorphia().map(E.class);
    }

    public static class R extends TestEntity {
    }

    public static class E extends TestEntity {
        @Reference
        @Serialized
        private R r;

    }
}
