package dev.morphia.test.mapping.validation.classrules;

import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.mapping.MappingException;
import dev.morphia.test.TestBase;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DuplicatePropertyNameTest extends TestBase {
    @Test
    public void testDuplicatedPropertyNameDifferentType() {
        Assertions.assertThrows(MappingException.class, () -> {
            getMapper().map(DuplicatedPropertyName2.class);
        });
    }

    @Test
    public void testDuplicatedPropertyNameSameType() {
        Assertions.assertThrows(MappingException.class, () -> {
            getMapper().map(DuplicatedPropertyName.class);
        });
    }

    @Entity
    private static class DuplicatedPropertyName {
        @Id
        private String id;

        @Property(value = "value")
        private String content1;
        @Property(value = "value")
        private String content2;
    }

    @Entity
    private static class DuplicatedPropertyName2 {
        @Id
        private String id;

        @Property(value = "value")
        private Map<String, Integer> content1;
        @Property(value = "value")
        private String content2;
    }

}
