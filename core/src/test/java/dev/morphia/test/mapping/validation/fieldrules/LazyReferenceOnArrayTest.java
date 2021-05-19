package dev.morphia.test.mapping.validation.fieldrules;


import dev.morphia.annotations.Reference;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.TestEntity;
import org.testng.annotations.Test;


@Test(groups = "references")
public class LazyReferenceOnArrayTest extends TestBase {

    @Test(expectedExceptions = ConstraintViolationException.class)
    public void testLazyRefOnArray() {
        getMapper().map(LazyOnArray.class);
    }

    private static class LazyOnArray extends TestEntity {
        @Reference(lazy = true)
        private R[] r;
    }

    private static class R extends TestEntity {
    }
}
