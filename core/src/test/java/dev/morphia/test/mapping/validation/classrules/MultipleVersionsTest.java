package dev.morphia.test.mapping.validation.classrules;

import dev.morphia.annotations.Version;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.TestEntity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MultipleVersionsTest extends TestBase {
    @Test
    public void testCheck() {
        Assertions.assertThrows(ConstraintViolationException.class, () -> {
            getMapper().map(OK1.class, Fail1.class);
        });
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
