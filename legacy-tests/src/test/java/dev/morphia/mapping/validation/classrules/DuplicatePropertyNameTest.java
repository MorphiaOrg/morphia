package dev.morphia.mapping.validation.classrules;


import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.mapping.validation.ConstraintViolationException;
import org.junit.Test;

import java.util.Map;

public class DuplicatePropertyNameTest extends TestBase {
    @Test(expected = ConstraintViolationException.class)
    public void testDuplicatedPropertyNameDifferentType() {
        getMapper().map(DuplicatedPropertyName2.class);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testDuplicatedPropertyNameSameType() {
        getMapper().map(DuplicatedPropertyName.class);
    }

    @Entity
    public static class DuplicatedPropertyName {
        @Id
        private String id;

        @Property(value = "value")
        private String content1;
        @Property(value = "value")
        private String content2;
    }

    @Entity
    public static class DuplicatedPropertyName2 {
        @Id
        private String id;

        @Property(value = "value")
        private Map<String, Integer> content1;
        @Property(value = "value")
        private String content2;
    }

}
