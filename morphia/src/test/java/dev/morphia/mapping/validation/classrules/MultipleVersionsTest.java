package dev.morphia.mapping.validation.classrules;


import dev.morphia.mapping.Mapper;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Version;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.testutil.TestEntity;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class MultipleVersionsTest extends TestBase {

    @Test(expected = ConstraintViolationException.class)
    public void testCheck() {
        Mapper.map(OK1.class);
        Mapper.map(Fail1.class);
    }

    public static class Fail1 extends TestEntity {
        @Version
        private long v1;
        @Version
        private long v2;
    }

    public static class OK1 extends TestEntity {
        @Version
        private long v1;
    }
}
