package dev.morphia.mapping.validation.fieldrules;


import dev.morphia.annotations.Entity;
import dev.morphia.mapping.Mapper;
import org.junit.Ignore;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.testutil.TestEntity;


@Ignore("references need some work")
public class LazyReferenceOnArrayTest extends TestBase {

    @Test(expected = ConstraintViolationException.class)
    public void testLazyRefOnArray() {
        getMapper().map(LazyOnArray.class);
    }

    public static class R extends TestEntity {
    }

    public static class LazyOnArray extends TestEntity {
        @Reference(lazy = true)
        private R[] r;

    }
}
