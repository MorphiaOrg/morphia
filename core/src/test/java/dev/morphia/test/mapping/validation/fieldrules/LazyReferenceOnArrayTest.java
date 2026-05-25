package dev.morphia.test.mapping.validation.fieldrules;

import dev.morphia.annotations.Reference;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.TestEntity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("references")
public class LazyReferenceOnArrayTest extends TestBase {

    @Test
    public void testLazyRefOnArray() {
        Assertions.assertThrows(ConstraintViolationException.class, () -> {
            getMapper().map(LazyOnArray.class);
        });
    }

    private static class LazyOnArray extends TestEntity {
        @Reference(lazy = true)
        private R[] r;
    }

    private static class R extends TestEntity {
    }
}
