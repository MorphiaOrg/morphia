package dev.morphia.test.mapping.validation.classrules;


import dev.morphia.annotations.Version;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.TestEntity;
import org.testng.annotations.Test;

public class MultipleVersionsTest extends TestBase {
    @Test(expectedExceptions = ConstraintViolationException.class)
    public void testCheck() {
        getMapper().map(OK1.class, Fail1.class);
    }

    private static class Fail1 extends TestEntity {
        @Version
        private long v1;
        @Version
        private long v2;
    }

    private static class OK1 extends TestEntity {
        @Version
        private long v1;
    }
}
