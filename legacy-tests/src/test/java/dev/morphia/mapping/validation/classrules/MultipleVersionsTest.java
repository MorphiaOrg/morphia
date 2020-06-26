package dev.morphia.mapping.validation.classrules;


import dev.morphia.TestBase;
import dev.morphia.annotations.Version;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.testutil.TestEntity;
import org.junit.Test;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class MultipleVersionsTest extends TestBase {

    @Test(expected = ConstraintViolationException.class)
    public void testCheck() {
        getMapper().map(OK1.class);
        getMapper().map(Fail1.class);
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
