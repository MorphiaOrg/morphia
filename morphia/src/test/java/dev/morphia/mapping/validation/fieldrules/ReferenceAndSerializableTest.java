package dev.morphia.mapping.validation.fieldrules;


import dev.morphia.mapping.Mapper;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Serialized;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.testutil.TestEntity;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ReferenceAndSerializableTest extends TestBase {
    @Test(expected = ConstraintViolationException.class)
    public void testCheck() {
        getMapper().map(E.class);
    }

    public static class R extends TestEntity {
    }

    public static class E extends TestEntity {
        @Reference
        @Serialized
        private R r;

    }
}
